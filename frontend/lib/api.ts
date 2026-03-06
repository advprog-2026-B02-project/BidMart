const BASE_URL = "http://localhost:8080";

async function parseError(res: Response) {
    try {
        const data = await res.json();
        return data.message || data.error || JSON.stringify(data);
    } catch {
        const text = await res.text();
        return text || `Error (${res.status})`;
    }
}

function getAccessToken() {
    if (typeof window === "undefined") return null;
    return localStorage.getItem("accessToken");
}

function getRefreshToken() {
    if (typeof window === "undefined") return null;
    return localStorage.getItem("refreshToken");
}

function setTokens(accessToken: string, refreshToken?: string) {
    if (typeof window === "undefined") return;
    localStorage.setItem("accessToken", accessToken);
    if (refreshToken) {
        localStorage.setItem("refreshToken", refreshToken);
    }
}

export async function logout() {
    if (typeof window === "undefined") return;

    const refreshToken = getRefreshToken();

    if (refreshToken) {
        try {
            await fetch(`${BASE_URL}/auth/logout`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({refreshToken}),
            });
        } catch {
        }
    }

    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
}

export async function register(email: string, password: string) {
    const res = await fetch(`${BASE_URL}/auth/register`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({email, password}),
    });

    if (!res.ok) {
        const message = await parseError(res);
        throw new Error(message || `Request failed (${res.status})`);
    }

    return res.text().catch(() => null);
}

export async function login(email: string, password: string) {
    const res = await fetch(`${BASE_URL}/auth/login`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({email, password}),
    });

    if (!res.ok) {
        const message = await parseError(res);
        throw new Error(message || `Request failed (${res.status})`);
    }

    const data = await res.json();

    const accessToken = data?.accessToken ?? data?.token ?? data?.jwt;
    const refreshToken = data?.refreshToken;

    if (!accessToken) {
        throw new Error("Login berhasil, tapi access token tidak ditemukan di response.");
    }

    setTokens(accessToken, refreshToken);

    return data;
}

export async function me() {
    const accessToken = getAccessToken();

    const res = await fetch(`${BASE_URL}/users/me`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            ...(accessToken ? {Authorization: `Bearer ${accessToken}`} : {}),
        },
    });

    if (!res.ok) {
        const message = await parseError(res);
        throw new Error(message || `Request failed (${res.status})`);
    }

    return res.json();
}