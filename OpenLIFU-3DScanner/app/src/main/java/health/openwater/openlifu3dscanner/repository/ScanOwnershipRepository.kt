package health.openwater.openlifu3dscanner.repository

import health.openwater.openlifu3dscanner.db.ScanOwnershipDao
import health.openwater.openlifu3dscanner.db.ScanOwnershipEntity
import health.openwater.openlifu3dscanner.network.api.AuthService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanOwnershipRepository @Inject constructor(
    private val dao: ScanOwnershipDao,
    private val authService: AuthService
) {
    /** Records the current user as owner of [collectionName]. No-op if not signed in. */
    suspend fun recordOwnership(collectionName: String) {
        val uid = authService.getCurrentUser()?.uid ?: return
        dao.insert(ScanOwnershipEntity(collectionName, uid))
    }

    /**
     * Returns the set of local collection names owned by the current user.
     * Returns empty set if not signed in.
     */
    suspend fun getOwnedCollectionNames(): Set<String> {
        val uid = authService.getCurrentUser()?.uid ?: return emptySet()
        return dao.getCollectionNamesForUser(uid).toSet()
    }

    /**
     * Claims any [candidates] that have no owner record yet, attributing them to the current user.
     * Used for one-time migration of pre-existing local scans.
     */
    suspend fun claimUnowned(candidates: Collection<String>) {
        val uid = authService.getCurrentUser()?.uid ?: return
        val alreadyOwned = dao.getAllOwnedCollectionNames().toSet()
        candidates
            .filter { it !in alreadyOwned }
            .forEach { dao.insert(ScanOwnershipEntity(it, uid)) }
    }
}
