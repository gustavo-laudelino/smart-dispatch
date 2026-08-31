import App from "../App";
import LoginPage from "../pages/LoginPage";
import { useAuth } from "./AuthContext";

function AuthGate() {
    const { sessao } = useAuth();

    if (!sessao) {
        return <LoginPage />;
    }

    return <App />;
}

export default AuthGate;
