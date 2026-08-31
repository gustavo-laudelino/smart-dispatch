import type { AuthSession } from "../types";

const CHAVE_SESSAO = "smart-dispatch:sessao";

export function salvarSessao(sessao: AuthSession): void {
    localStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessao));
}

export function lerSessao(): AuthSession | null {
    const bruto = localStorage.getItem(CHAVE_SESSAO);

    if (!bruto) {
        return null;
    }

    try {
        return JSON.parse(bruto) as AuthSession;
    } catch {
        return null;
    }
}

export function obterToken(): string | null {
    return lerSessao()?.token ?? null;
}

export function limparSessao(): void {
    localStorage.removeItem(CHAVE_SESSAO);
}
