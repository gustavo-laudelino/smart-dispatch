package br.com.smartdispatch.dto;

public class TecnicoResponse {

    private Long id;
    private String nome;
    private String email;
    private String telefone;
    private String perfil;
    private boolean ativo;

    private Long baseId;
    private String baseNome;

    private Long contratoId;
    private String contratoCidade;

    public TecnicoResponse() {
    }

    public TecnicoResponse(
            Long id,
            String nome,
            String email,
            String telefone,
            String perfil,
            boolean ativo,
            Long baseId,
            String baseNome,
            Long contratoId,
            String contratoCidade
    ) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.perfil = perfil;
        this.ativo = ativo;
        this.baseId = baseId;
        this.baseNome = baseNome;
        this.contratoId = contratoId;
        this.contratoCidade = contratoCidade;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getPerfil() {
        return perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Long getBaseId() {
        return baseId;
    }

    public String getBaseNome() {
        return baseNome;
    }

    public Long getContratoId() {
        return contratoId;
    }

    public String getContratoCidade() {
        return contratoCidade;
    }
}