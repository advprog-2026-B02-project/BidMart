const BASE_URL = "http://localhost:8080";

async function parseError(res: Response | null) {
    if (!res || res.status === 0) {
        return "Gagal terhubung ke server. Pastikan koneksi internet aktif.";
    }

    try {
        const text = await res.text();
        let data;

        try {
            data = JSON.parse(text);
        } catch {
            if (text.includes("<html>")) return `Terjadi kesalahan pada server (Kode: ${res.status}).`;
            return text || `Permintaan gagal (Kode: ${res.status})`;
        }

        const rawMsg = data?.message || data?.error || "";

        if (rawMsg.includes("already used")) {
            return "Link reset password ini sudah pernah digunakan.";
        }
        if (rawMsg.includes("expired")) {
            return "Link reset password sudah kadaluarsa.";
        }
        if (rawMsg.includes("Invalid reset token") || rawMsg.includes("Invalid password token")) {
            return "Link reset tidak valid atau tidak ditemukan.";
        }

        if (rawMsg.includes("Invalid credentials") || rawMsg.includes("Bad credentials")) {
            return "Email atau kata sandi salah.";
        }
        if (rawMsg.includes("Email not found")) {
            return "Email tidak terdaftar di sistem kami.";
        }
        if (rawMsg.includes("Email already registered")) {
            return "Email sudah terdaftar. Silakan masuk atau gunakan email lain.";
        }
        if (rawMsg.includes("Token already used") && !rawMsg.includes("Reset")) {
            return "Akun Anda sudah terverifikasi. Silakan login.";
        }

        return rawMsg || `Terjadi kesalahan (Kode: ${res.status})`;
    } catch {
        return "Terjadi kesalahan sistem yang tidak terduga.";
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
            throw e; // just throw it
        }
    }

    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
}

export async function register(email: string, password: string, displayName: string) {
    try {
        const res = await fetch(`${BASE_URL}/auth/register`, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({email, password, displayName}),
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

export async function forgotPassword(email: string) {
    try {
        const res = await fetch(`${BASE_URL}/auth/forgot-password`, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({email}),
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

export async function resetPassword(token: string, newPass: string) {
    try {
        const res = await fetch(`${BASE_URL}/auth/reset-password`, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({token, newPassword: newPass}),
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

export async function validateResetToken(token: string) {
    try {
        const res = await fetch(`${BASE_URL}/auth/reset-password/validate?token=${token}`, {
            method: "GET",
        });

        if (!res.ok) {
            const message = await parseError(res);
            throw new Error(message);
        }

        return true;
    } catch (err: any) {
        if (!(err instanceof Error) || err.message === "Failed to fetch") {
            const cleanMsg = await parseError(null);
            throw new Error(cleanMsg);
        }
        throw err;
    }
}

export async function updateProfile(displayName: string, avatarUrl: string) {
    try {
        const token = getAccessToken();

        if (!token) {
            throw new Error("Sesi tidak ditemukan, silakan login ulang.");
        }

        const res = await fetch(`${BASE_URL}/me`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify({displayName, avatarUrl}),
        });

        if (!res.ok) {
            const msg = await parseError(res);
            throw new Error(msg);
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