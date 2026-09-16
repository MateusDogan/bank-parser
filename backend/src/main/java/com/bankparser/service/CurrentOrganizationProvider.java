package com.bankparser.service;

import com.bankparser.entity.Organization;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * De qual organizacao e a requisicao atual.
 *
 * <p>Sem autenticacao no MVP, sempre a organizacao padrao. Na Fase 7 esta
 * classe passa a ler do {@code SecurityContext} — e por existir esta costura,
 * em um lugar so, nenhum controller precisa mudar quando isso acontecer.
 */
@Component
public class CurrentOrganizationProvider {

    public UUID currentOrganizationId() {
        return Organization.DEFAULT_ID;
    }
}
