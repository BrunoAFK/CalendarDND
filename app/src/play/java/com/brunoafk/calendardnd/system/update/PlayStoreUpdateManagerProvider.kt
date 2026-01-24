package com.brunoafk.calendardnd.system.update

object PlayStoreUpdateManagerProvider {
    fun get(): PlayStoreUpdateManager {
        return PlayStoreUpdateManagerImpl()
    }
}
