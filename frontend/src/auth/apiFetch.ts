import { limparSessao, obterToken } from "./authStorage";

type OuvinteSessaoExpirada = () => void;

let ouvinteSessaoExpirada: OuvinteSessaoExpirada = () => {};

export function definirOuvinteSessaoExpirada(
    ouvinte: OuvinteSessaoExpirada
): void {
    ouvinteSessaoExpirada = ouvinte;
}

export async function apiFetch(
    input: string,
    init: RequestInit = {}
): Promise<Response> {
    const token = obterToken();

    const headers = new Headers(init.headers);

    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    const response = await fetch(input, {
        ...init,
        headers,
    });

    if (response.status === 401) {
        limparSessao();
        ouvinteSessaoExpirada();
    }

    return response;
}
