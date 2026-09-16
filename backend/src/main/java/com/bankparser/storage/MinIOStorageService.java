package com.bankparser.storage;

import com.bankparser.config.MinIOProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class MinIOStorageService implements StorageService {

    private final MinioClient client;
    private final String bucket;

    public MinIOStorageService(MinioClient client, MinIOProperties properties) {
        this.client = client;
        this.bucket = properties.bucket();
    }

    /**
     * O docker-compose nao cria o bucket, e a primeira gravacao falharia sem
     * ele. Feito no upload, e nao no start da aplicacao, para que a API suba
     * mesmo com o storage momentaneamente fora do ar — so o upload depende dele.
     * O custo e uma chamada extra por upload, irrelevante num fluxo em que quem
     * dispara e uma pessoa enviando um PDF.
     */
    private void ensureBucketExists() {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new StorageException("Falha ao preparar o bucket '" + bucket + "'", e);
        }
    }

    @Override
    public void upload(String objectKey, InputStream content, long size, String contentType) {
        ensureBucketExists();
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(content, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Falha ao enviar '" + objectKey + "' para o storage", e);
        }
    }

    @Override
    public InputStream download(String objectKey) {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Falha ao ler '" + objectKey + "' do storage", e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Falha ao remover '" + objectKey + "' do storage", e);
        }
    }
}
