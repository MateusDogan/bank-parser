package com.bankparser.storage;

import java.io.InputStream;

/**
 * Armazenamento de arquivos (PDFs enviados e, depois, exports gerados).
 *
 * <p>Nenhum tipo do MinIO aparece nesta interface de proposito: trocar por AWS
 * S3 ou Cloudflare R2 deve custar uma implementacao nova e configuracao, sem
 * tocar em servico ou controller.
 */
public interface StorageService {

    void upload(String objectKey, InputStream content, long size, String contentType);

    InputStream download(String objectKey);

    void delete(String objectKey);
}
