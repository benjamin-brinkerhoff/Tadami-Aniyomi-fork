package mihon.domain.extensionrepo.anime.interactor

import mihon.domain.extensionrepo.model.ExtensionRepo
import mihon.domain.extensionstore.anime.repository.AnimeExtensionStoreRepository
import mihon.domain.extensionstore.model.legacyBaseUrl

class ReplaceAnimeExtensionRepo(
    private val repository: AnimeExtensionStoreRepository,
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
