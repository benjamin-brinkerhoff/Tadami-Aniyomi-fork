package mihon.domain.extensionstore.repository

import kotlinx.coroutines.flow.Flow
import mihon.domain.extensionstore.model.ExtensionStore

interface ExtensionStoreRepository {
    suspend fun insert(indexUrl: String): Result<Unit>

    suspend fun insertFromPreference(indexUrl: String, name: String)

    suspend fun refreshAll()

    suspend fun upsertStore(store: ExtensionStore)

    suspend fun getAll(): List<ExtensionStore>

    fun getAllAsFlow(): Flow<List<ExtensionStore>>

    fun getCountAsFlow(): Flow<Long>

    suspend fun remove(indexUrl: String)

    /**
     * One-time migration helper from legacy repos. Default no-op for implementations
     * that don't need it. Data impls override to do the port if store table is empty.
     * Called only from extension fetch paths (deferred after first frame for perf).
     */
    suspend fun ensureLegacyMigrated() {}
}
