package mihon.domain.extensionrepo.manga.interactor

import mihon.domain.extensionrepo.model.ExtensionRepo
import mihon.domain.extensionstore.manga.repository.MangaExtensionStoreRepository
import mihon.domain.extensionstore.model.legacyBaseUrl

class ReplaceMangaExtensionRepo(
    private val repository: MangaExtensionStoreRepository,
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
