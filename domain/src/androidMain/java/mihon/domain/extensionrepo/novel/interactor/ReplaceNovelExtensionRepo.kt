package mihon.domain.extensionrepo.novel.interactor

import mihon.domain.extensionrepo.model.ExtensionRepo
import mihon.domain.extensionstore.model.legacyBaseUrl
import mihon.domain.extensionstore.novel.repository.NovelExtensionStoreRepository

class ReplaceNovelExtensionRepo(
    private val repository: NovelExtensionStoreRepository,
) {
    suspend fun await(repo: ExtensionRepo) {
        val normalized = repo.baseUrl.trimEnd('/')
        val existing = repository.getAll().find { it.legacyBaseUrl().trimEnd('/') == normalized }
            ?: return
        repository.upsertStore(
            existing.copy(
                name = repo.name,
                badgeLabel = repo.shortName ?: repo.name,
                contact = existing.contact.copy(website = repo.website),
                signingKey = repo.signingKeyFingerprint,
            ),
        )
    }
}
