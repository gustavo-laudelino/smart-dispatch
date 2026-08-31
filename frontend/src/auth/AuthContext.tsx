import {
    createContext,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useState,
} from "react";
import type { ReactNode } from "react";

import { lerSessao, limparSessao, salvarSessao } from "./authStorage";
import { definirOuvinteSessaoExpirada } from "./apiFetch";

import type { AuthSession } from "../types";

type AuthContextValue = {
    sessao: AuthSession | null;
    autenticado: boolean;
    login: (sessao: AuthSession) => void;
    logout: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider(
    { children }: { children: ReactNode }
) {
    const [sessao, setSessao] = useState<AuthSession | null>(
        () => lerSessao()
    );

    const login = useCallback((novaSessao: AuthSession) => {
        salvarSessao(novaSessao);
        setSessao(novaSessao);
    }, []);

    const logout = useCallback(() => {
        limparSessao();
        setSessao(null);
    }, []);

    useEffect(() => {
        definirOuvinteSessaoExpirada(() => {
            setSessao(null);
        });

        return () => {
            definirOuvinteSessaoExpirada(() => {});
        };
    }, []);

    const value = useMemo<AuthContextValue>(
        () => ({
            sessao,
            autenticado: sessao !== null,
            login,
            logout,
        }),
        [sessao, login, logout]
    );

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
}

// eslint-disable-next-line react-refresh/only-export-components -- Context, Provider e hook ficam juntos por decisão desta rodada (um único arquivo)
export function useAuth(): AuthContextValue {
    const context = useContext(AuthContext);

    if (context === undefined) {
        throw new Error(
            "useAuth deve ser usado dentro de um AuthProvider"
        );
    }

    return context;
}
