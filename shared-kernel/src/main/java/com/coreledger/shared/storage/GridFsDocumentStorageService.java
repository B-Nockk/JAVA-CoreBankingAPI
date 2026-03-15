// shared-kernel/src/main/java/com/coreledger/shared/storage/GridFsDocumentStorageService.java
package com.coreledger.shared.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.data.mongodb.core.query.Query;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.client.gridfs.model.GridFSFile;

@Component
public class GridFsDocumentStorageService implements DocumentStorageService {

    private final GridFsTemplate gridFsTemplate;

    public GridFsDocumentStorageService(GridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }

    @Override
    public String save(byte[] content, String filename, String mimeType) {
        ObjectId id = gridFsTemplate.store(new ByteArrayInputStream(content), filename, mimeType);
        return id.toHexString(); // storagePath = ObjectId string
    }

    @Override
    public byte[] load(String storagePath) {
        GridFSFile file = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(new ObjectId(storagePath))));
        try (InputStream in = gridFsTemplate.getResource(file).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load document from GridFS", e);
        }
    }

    @Override
    public void delete(String storagePath) {
        gridFsTemplate.delete(Query.query(Criteria.where("_id").is(new ObjectId(storagePath))));
    }
}
