package org.usth.ict.ulake.textr.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.usth.ict.ulake.textr.models.IndexFiles;
import org.usth.ict.ulake.textr.models.IndexingStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndexFilesRepository extends PagingAndSortingRepository<IndexFiles, Long> {
    
    @Modifying
    @Query("update IndexFiles ifs set ifs.status = :status where ifs.fileId = :fid")
    void updateStatusByFileId(Long fid, IndexingStatus status);
    
    Optional<IndexFiles> findByFileId(Long fid);
    
    Optional<IndexFiles> findByCoreIdAndStatus(String coreId, IndexingStatus status);
    
    List<IndexFiles> findAllByStatus(IndexingStatus status, Pageable pageable);
    
    List<IndexFiles> findAllByStatusIn(List<IndexingStatus> statuses, Pageable pageable);
}
