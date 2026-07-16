package br.com.smartdispatch.dto;

import br.com.smartdispatch.enums.StatusChamado;

public class StatusChamadoRequest {

    private StatusChamado status;

    public StatusChamadoRequest() {
    }

    public StatusChamado getStatus() {
        return status;
    }

    public void setStatus(StatusChamado status) {
        this.status = status;
    }
}