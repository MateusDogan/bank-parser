package com.bankparser.testsupport;

import com.bankparser.storage.StorageException;
import com.bankparser.storage.StorageService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link StorageService} em memoria para os testes, no lugar do MinIO (que
 * exige Docker). Guarda os bytes de verdade, entao o teste consegue afirmar que
 * o arquivo foi gravado — e nao apenas que um metodo foi chamado.
 */
public class InMemoryStorageService implements StorageService {

    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public void upload(String objectKey, InputStream content, long size, String contentType) {
        try {
            objects.put(objectKey, content.readAllBytes());
        } catch (IOException e) {
            throw new StorageException("Falha ao gravar '" + objectKey + "'", e);
        }
    }

    @Override
    public InputStream download(String objectKey) {
        byte[] content = objects.get(objectKey);
        if (content == null) {
            throw new StorageException("Objeto inexistente: " + objectKey, null);
        }
        return new ByteArrayInputStream(content);
    }

    @Override
    public void delete(String objectKey) {
        objects.remove(objectKey);
    }

    public Set<String> keys() {
        return objects.keySet();
    }

    public void clear() {
        objects.clear();
    }
}
