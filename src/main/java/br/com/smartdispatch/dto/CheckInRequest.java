package br.com.smartdispatch.dto;

public class CheckInRequest {

    private Boolean encerrarCheckInAnterior;

    public CheckInRequest() {
    }

    public Boolean getEncerrarCheckInAnterior() {
        return encerrarCheckInAnterior;
    }

    public void setEncerrarCheckInAnterior(
            Boolean encerrarCheckInAnterior
    ) {
        this.encerrarCheckInAnterior = encerrarCheckInAnterior;
    }
}