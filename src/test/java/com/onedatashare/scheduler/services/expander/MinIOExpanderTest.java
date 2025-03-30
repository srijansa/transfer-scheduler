package com.onedatashare.scheduler.services.expanders;

import com.onedatashare.scheduler.model.EntityInfo;
import com.onedatashare.scheduler.model.credential.AccountEndpointCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MinIOExpanderTest {

    private MinIOExpander expander;

    @BeforeEach
    public void setup() {
        expander = new MinIOExpander();
        expander.createClient(createValidCredential());
    }

    private AccountEndpointCredential createValidCredential() {
        AccountEndpointCredential cred = new AccountEndpointCredential();
        cred.setUsername("admin");
        cred.setSecret("admin123");
        cred.setCustomEndpoint("http://localhost:9000");
        cred.setUri("us-east-1:::test-bucket");
        return cred;
    }

    private List<EntityInfo> mockFolderSelection() {
        EntityInfo folder = new EntityInfo();
        folder.setId("test-folder");
        folder.setPath("test-folder/");
        List<EntityInfo> list = new ArrayList<>();
        list.add(folder);
        return list;
    }

    @Test
    public void testCreateClientWithValidCredential() {
        assertDoesNotThrow(() -> expander.createClient(createValidCredential()));
        assertNotNull(expander.getMinioClient(), "MinIO client should not be null");
    }

    @Test
    public void testDestinationChunkSizeAppliesChunkProperly() {
        List<EntityInfo> input = new ArrayList<>();
        EntityInfo file = new EntityInfo();
        file.setId("some-file");
        file.setPath("some-file");
        file.setSize(12345L);
        input.add(file);

        int customChunk = 12000000;
        List<EntityInfo> result = expander.destinationChunkSize(input, "", customChunk);

        assertEquals(customChunk, result.get(0).getChunkSize(), "Chunk size should be applied correctly");
    }
    @Test
    public void testExpandFileSystemOnRoot() {
        List<EntityInfo> result = expander.expandedFileSystem(new ArrayList<>(), "");
        assertNotNull(result, "Result list should not be null");
        assertFalse(result.isEmpty(), "please add files and folders");

        System.out.println("----- Files/Folders at root -----");
        for (EntityInfo file : result) {
            System.out.println("ID: " + file.getId() + ", Path: " + file.getPath() + ", Size: " + file.getSize());
            assertNotNull(file.getId(), "File ID should not be null");
            assertNotNull(file.getPath(), "File path should not be null");
        }
    }
    @Test
    public void testExpandFileSystemOnSubfolder() {
        List<EntityInfo> result = expander.expandedFileSystem(mockFolderSelection(), "test-folder/");
        assertNotNull(result, "Result list should not be null");
        assertFalse(result.isEmpty(), "Folder is empty");

        System.out.println("----- Files/Folders in 'test-folder/' -----");
        for (EntityInfo file : result) {
            System.out.println("ID: " + file.getId() + ", Path: " + file.getPath() + ", Size: " + file.getSize());
            assertTrue(file.getPath().startsWith("test-folder/"), "Path under 'test-folder/'");
        }
    }

}
