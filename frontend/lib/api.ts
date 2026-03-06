const BASE_URL = "http://localhost:8080";

async function parseError(res: Response | null) {
    if (!res || res.status === 0) {
        return "Gagal terhubung ke server. Pastikan koneksi internet aktif.";
    }

    try {
        const data = await res.json();
        const rawMsg = data?.message || data?.error || "";

        if (rawMsg.includes("Bad credentials") || rawMsg.includes("Invalid credentials")) {
            return "Email atau kata sandi yang Anda masukkan salah.";
        }
        if (rawMsg.includes("Email already registered")) {
            return "Email ini sudah terdaftar. Silakan gunakan email lain atau masuk.";
        }
        if (rawMsg.includes("not verified") || rawMsg.includes("disabled")) {
            return "Akun Anda belum aktif. Silakan cek email untuk verifikasi.";
        }
        if (rawMsg.includes("expired") || rawMsg.includes("Invalid token")) {
            return "Link verifikasi sudah kadaluarsa. Silakan daftar ulang atau minta link baru.";
        }
        if (rawMsg.includes("expired") || rawMsg.includes("Invalid token")) {
            return "Link verifikasi sudah kadaluarsa. Silakan daftar ulang atau minta link baru.";
        }
        if (rawMsg.includes("already used")) {
            return "Akun anda sudah terdaftar. Silakan masuk ke akun Anda.";
        }

        return rawMsg || `Terjadi kesalahan (Kode: ${res.status})`;
    } catch {
        try {
            const text = await res.text();
            if (text.includes("<html>")) return `Terjadi kesalahan sistem (${res.status})`;
            return text || `Permintaan gagal (${res.status})`;
        } catch {
            return "Terjadi kesalahan yang tidak terduga.";
        }
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
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({refreshToken}),
            });
        } catch (e) {
        }
    }

    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
}

export async function register(email: string, password: string) {
    try {
        const res = await fetch(`${BASE_URL}/auth/register`, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({email, password}),
        });

        if (!res.ok) {
            const message = await parseError(res);
            throw new Error(message);
        }

        return await res.text();
    } catch (err: any) {
        if (!(err instanceof Error) || err.message === "Failed to fetch") {
            const cleanMsg = await parseError(null);
            throw new Error(cleanMsg);
        }
        throw err;
    }
}

export async function login(email: string, password: string) {
    try {
        const res = await fetch(`${BASE_URL}/auth/login`, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({email, password}),
        });

        if (!res.ok) {
            const message = await parseError(res);
            throw new Error(message);
        }

        const data = await res.json();
        const accessToken = data?.accessToken ?? data?.token ?? data?.jwt;
        const refreshToken = data?.refreshToken;

        if (!accessToken) {
            throw new Error("Login berhasil, tapi token tidak ditemukan.");
        }

        setTokens(accessToken, refreshToken);
        return data;
    } catch (err: any) {
        if (!(err instanceof Error) || err.message === "Failed to fetch") {
            const cleanMsg = await parseError(null);
            throw new Error(cleanMsg);
        }
        throw err;
    }
}

export async function me() {
    try {
        const accessToken = getAccessToken();
        const res = await fetch(`${BASE_URL}/users/me`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                ...(accessToken ? {Authorization: `Bearer ${accessToken}`} : {}),
            },
        });

        if (res.status === 401) {
            logout();
            if (typeof window !== "undefined") window.location.href = "/login";
            throw new Error("Sesi Anda telah berakhir. Silakan masuk kembali.");
        }

        if (!res.ok) {
            const message = await parseError(res);
            throw new Error(message);
        }

        return await res.json();
    } catch (err: any) {
        if (!(err instanceof Error) || err.message === "Failed to fetch") {
            const cleanMsg = await parseError(null);
            throw new Error(cleanMsg);
        }
        throw err;
    }
}

export async function verifyEmail(token: string) {
    try {
        const res = await fetch(`${BASE_URL}/auth/verify?token=${token}`, {
            method: "GET",
        });

        if (!res.ok) {
            const message = await parseError(res);
            throw new Error(message);
        }

        return await res.text();
    } catch (err: any) {
        if (!(err instanceof Error) || err.message === "Failed to fetch") {
            const cleanMsg = await parseError(null);
            throw new Error(cleanMsg);
        }
        throw err;
    }
}