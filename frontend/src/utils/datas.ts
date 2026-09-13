function paraISO(data: Date): string {
    const ano = data.getFullYear();
    const mes = String(data.getMonth() + 1).padStart(2, "0");
    const dia = String(data.getDate()).padStart(2, "0");

    return `${ano}-${mes}-${dia}`;
}

export function hojeISO(): string {
    return paraISO(new Date());
}

export function adicionarDias(
    dataISO: string,
    quantidade: number
): string {
    const [ano, mes, dia] = dataISO
        .split("-")
        .map(Number);

    const data = new Date(ano, mes - 1, dia);
    data.setDate(data.getDate() + quantidade);

    return paraISO(data);
}

// Segunda-feira como início da semana.
export function inicioDaSemana(
    dataISO: string
): string {
    const [ano, mes, dia] = dataISO
        .split("-")
        .map(Number);

    const data = new Date(ano, mes - 1, dia);
    const diaSemana = data.getDay();

    const deslocamento =
        diaSemana === 0 ? -6 : 1 - diaSemana;

    data.setDate(data.getDate() + deslocamento);

    return paraISO(data);
}

export function formatarDataCurta(
    dataISO: string
): string {
    const [ano, mes, dia] = dataISO
        .split("-")
        .map(Number);

    return new Date(ano, mes - 1, dia).toLocaleDateString(
        "pt-BR",
        {
            day: "2-digit",
            month: "2-digit",
        }
    );
}

export function formatarDiaSemana(
    dataISO: string
): string {
    const [ano, mes, dia] = dataISO
        .split("-")
        .map(Number);

    const rotulo = new Date(
        ano,
        mes - 1,
        dia
    ).toLocaleDateString("pt-BR", {
        weekday: "long",
    });

    return (
        rotulo.charAt(0).toUpperCase() +
        rotulo.slice(1)
    );
}

export function formatarHora(
    hora: string | null
): string {
    if (!hora) {
        return "Sem horário";
    }

    return hora.slice(0, 5);
}
