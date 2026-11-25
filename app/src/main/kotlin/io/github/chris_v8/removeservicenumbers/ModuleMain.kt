// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Chris (chris-v8)

package io.github.chris_v8.removeservicenumbers

import android.util.Log
import io.github.libxposed.api.XposedInterface.ExceptionMode
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.lang.reflect.Method

/**
 * Keeps the SIM card's service numbers (EF_SDN) out of the contacts database on ColorOS.
 *
 * The Oplus ContactsProvider wipes its "SDN Account" contacts every time it starts and then
 * copies the service numbers from each SIM again through
 * `AbstractSimOverlayManager.insertSimContacts(account, contacts)`. This module skips that call
 * for service-number accounts, so after the next restart of the provider (a reboot) they are gone.
 * Normal SIM contacts use the same method and are left alone.
 */
class ModuleMain : XposedModule() {

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != CONTACTS_PROVIDER) return
        // Never let a problem here escape: android.process.acore serves contacts to every app.
        try {
            hookInsertSimContacts(param.classLoader)
        } catch (t: Throwable) {
            log(Log.ERROR, LOG_TAG, "Could not install the hook; service numbers stay visible", t)
        }
    }

    private fun hookInsertSimContacts(classLoader: ClassLoader) {
        val manager = classLoader.loadClass(SIM_OVERLAY_MANAGER)
        val insert = manager.declaredMethods.singleOrNull {
            it.name == "insertSimContacts" && it.parameterTypes.size == 2
        } ?: error("$SIM_OVERLAY_MANAGER.insertSimContacts(account, contacts) not found")
        val getAccountType = insert.parameterTypes[0].getMethod("getAccountType")

        hook(insert)
            // If the hook itself fails, the original method runs as if there were no hook.
            .setExceptionMode(ExceptionMode.PROTECTIVE)
            .intercept { chain ->
                val accountType = accountTypeOf(getAccountType, chain.getArg(0))
                if (accountType in SERVICE_NUMBER_ACCOUNT_TYPES) {
                    val count = (chain.getArg(1) as? Array<*>)?.size ?: 0
                    log(Log.INFO, LOG_TAG, "Skipped $count service number(s) for $accountType")
                    null // insertSimContacts returns nothing
                } else {
                    chain.proceed()
                }
            }
        log(Log.INFO, LOG_TAG, "Hooked ${insert.declaringClass.name}.${insert.name}")
    }

    private fun accountTypeOf(getAccountType: Method, account: Any?): String? =
        account?.let { getAccountType.invoke(it) as? String }

    private companion object {
        // No tag of our own: Vector then logs under its own tag with the module name in front,
        // and only its own tags are saved to the module log shown in the manager.
        val LOG_TAG: String? = null
        const val CONTACTS_PROVIDER = "com.android.providers.contacts"
        const val SIM_OVERLAY_MANAGER = "com.customize.manager.AbstractSimOverlayManager"

        /** Account types ColorOS and other ROMs use for SIM service numbers. */
        val SERVICE_NUMBER_ACCOUNT_TYPES = setOf(
            "SDN Account",
            "SDN Account SPRD",
            "com.android.contacts.sdn",
        )
    }
}
