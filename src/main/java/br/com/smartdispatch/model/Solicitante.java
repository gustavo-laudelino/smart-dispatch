package br.com.smartdispatch.model;

public class Solicitante {

    private String nome;
    private String email;
    private String telefone;
    private String matriucula;

    public Solicitante() {
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getMatriucula() {
        return matriucula;
    }

    public void setMatricula(String matriucula) {
        this.matriucula = matriucula;
    }
}
