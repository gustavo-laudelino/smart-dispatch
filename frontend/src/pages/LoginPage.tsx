import { useState } from "react";
import type { FormEvent } from "react";

import { login as loginApi } from "../api";
import { useAuth } from "../auth/AuthContext";

import type { AuthSession } from "../types";

function LoginPage() {
    const { login } = useAuth();

    const [email, setEmail] = useState("");
    const [senha, setSenha] = useState("");
    const [carregando, setCarregando] = useState(false);
    const [erro, setErro] = useState<string | null>(null);

    async function aoEnviar(evento: FormEvent<HTMLFormElement>) {
        evento.preventDefault();

        if (carregando) {
            return;
        }

        if (
            email.trim().length === 0 ||
            senha.trim().length === 0
        ) {
            setErro("E-mail e senha são obrigatórios.");
            return;
        }

        setErro(null);
        setCarregando(true);

        try {
            const resposta = await loginApi({
                email,
                senha,
            });

            const sessao: AuthSession = {
                token: resposta.token,
                usuarioId: resposta.usuarioId,
                nome: resposta.nome,
                email: resposta.email,
                perfil: resposta.perfil,
            };

            login(sessao);
        } catch (error) {
            setErro(
                error instanceof Error
                    ? error.message
                    : "Ocorreu um erro inesperado"
            );

            setCarregando(false);
        }
    }

    return (
        <div className="login-page">
            <form className="login-card" onSubmit={aoEnviar}>
                <div className="login-brand">
                    <div className="login-brand-mark">SD</div>

                    <h1>Smart Dispatch</h1>
                </div>

                <p className="login-subtitle">
                    Entre com suas credenciais para acessar a operação.
                </p>

                {erro && (
                    <div className="error login-error">{erro}</div>
                )}

                <div className="form-field">
                    <label htmlFor="login-email">E-mail</label>

                    <input
                        id="login-email"
                        type="email"
                        autoComplete="username"
                        value={email}
                        onChange={(evento) =>
                            setEmail(evento.target.value)
                        }
                    />
                </div>

                <div className="form-field">
                    <label htmlFor="login-senha">Senha</label>

                    <input
                        id="login-senha"
                        type="password"
                        autoComplete="current-password"
                        value={senha}
                        onChange={(evento) =>
                            setSenha(evento.target.value)
                        }
                    />
                </div>

                <button
                    type="submit"
                    className="primary-button login-submit"
                    disabled={carregando}
                >
                    {carregando ? "Entrando..." : "Entrar"}
                </button>
            </form>
        </div>
    );
}

export default LoginPage;
