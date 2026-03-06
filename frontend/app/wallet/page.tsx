"use client";

import {useCallback, useEffect, useMemo, useState} from "react";
import Link from "next/link";
import {me} from "@/lib/api";

const BACKEND_URL =
    process.env.NEXT_PUBLIC_BACKEND_URL ?? "http://localhost:8080";

type WalletResponse = {
    userId: string;
    availableBalance: number;
    heldBalance: number;
    totalBalance: number;
    updatedAt?: string | null;
};

type HoldResponse = {
    holdId: string;
    userId: string;
    amount: number;
    status: string;
    createdAt?: string;
};

const parseError = (error: unknown) => {
    if (error instanceof Error) {
        return error.message;
    }
    if (typeof error === "string") {
        return error;
    }
    return "Unexpected error";
};

const authRequest = async <T,>(path: string, init?: RequestInit): Promise<T> => {
    const token =
        typeof window !== "undefined" ? localStorage.getItem("accessToken") : null;

    const headers: Record<string, string> = {
        ...(init?.headers as Record<string, string> | undefined),
    };

    if (init?.body && !headers["Content-Type"]) {
        headers["Content-Type"] = "application/json";
    }
    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    const response = await fetch(`${BACKEND_URL}${path}`, {
        cache: "no-store",
        ...init,
        headers,
    });
    if (!response.ok) {
        const message = await response.text().catch(() => "");
        throw new Error(message || response.statusText);
    }
    return (await response.json()) as T;
};

interface SessionInfo {
    userId: string;
    email: string;
}

export default function WalletPage() {
    const [session, setSession] = useState<SessionInfo | null>(null);
    const [sessionMsg, setSessionMsg] = useState<string>("Checking session...");

    const [wallet, setWallet] = useState<WalletResponse | null>(null);
    const [activeHold, setActiveHold] = useState<HoldResponse | null>(null);
    const [topUpInput, setTopUpInput] = useState("100000");
    const [bidInput, setBidInput] = useState("50000");
    const [raiseInput, setRaiseInput] = useState("10000");
    const [status, setStatus] = useState("Wallet not created yet.");
    const [syncing, setSyncing] = useState(false);
    const [acting, setActing] = useState(false);

    useEffect(() => {
        (async () => {
            try {
                const data = await me();
                const info = {
                    userId: String(data?.userId ?? ""),
                    email: data?.email ?? "user",
                };
                setSession(info);
                setSessionMsg(`Logged in as ${info.email}`);
            } catch (error) {
                setSession(null);
                setSessionMsg(
                    parseError(error) ||
                    "You need to login first to use the wallet simulator.",
                );
            }
        })();
    }, []);

    const formatAmount = useMemo(
        () =>
            new Intl.NumberFormat("id-ID", {
                style: "currency",
                currency: "IDR",
                minimumFractionDigits: 0,
            }),
        [],
    );

    const syncWallet = useCallback(async () => {
        if (!session) {
            return;
        }
        setSyncing(true);
        try {
            const data = await authRequest<WalletResponse>("/api/wallets/me");
            setWallet(data);
            setStatus("Wallet synced with backend.");
        } catch (error) {
            setStatus(parseError(error));
        } finally {
            setSyncing(false);
        }
    }, [session]);

    useEffect(() => {
        if (session) {
            syncWallet();
        }
    }, [session, syncWallet]);

    const runAction = useCallback(
        async (message: string, action: () => Promise<unknown>) => {
            if (!session) {
                setStatus("Please login first.");
                return;
            }
            setActing(true);
            try {
                await action();
                await syncWallet();
                setStatus(message);
            } catch (error) {
                setStatus(parseError(error));
            } finally {
                setActing(false);
            }
        },
        [session, syncWallet],
    );

    const handleTopUp = () => {
        const amount = Number(topUpInput);
        if (!Number.isFinite(amount) || amount <= 0) {
            setStatus("Top up amount must be positive.");
            return;
        }
        runAction(`Top up successful (+${formatAmount.format(amount)}).`, () =>
            authRequest<WalletResponse>("/api/wallets/me/top-up", {
                method: "POST",
                body: JSON.stringify({amount}),
            }),
        );
    };

    const handleBid = (amount: number, label: string) => {
        if (!Number.isFinite(amount) || amount <= 0) {
            setStatus("Bid amount must be positive.");
            return;
        }
        runAction(label, async () => {
            const response = await authRequest<HoldResponse>(
                "/api/wallets/me/holds",
                {
                    method: "POST",
                    body: JSON.stringify({
                        amount,
                    }),
                },
            );
            setActiveHold(response);
        });
    };

    const handleReleaseHold = () => {
        if (!activeHold) {
            setStatus("No active hold to release.");
            return;
        }
        runAction("Hold released.", async () => {
            await authRequest<HoldResponse>(
                `/api/wallets/holds/${activeHold.holdId}/release`,
                {
                    method: "POST",
                },
            );
            setActiveHold(null);
        });
    };

    const handleReset = () => {
        runAction("Wallet reset to zero.", async () => {
            await authRequest<WalletResponse>("/api/wallets/me/reset", {
                method: "POST",
            });
            setActiveHold(null);
        });
    };

    const handleRaiseBid = () => {
        if (!activeHold) {
            setStatus("Place a bid before raising.");
            return;
        }
        const increment = Number(raiseInput);
        if (!Number.isFinite(increment) || increment <= 0) {
            setStatus("Raise amount must be positive.");
            return;
        }
        const newAmount = activeHold.amount + increment;
        runAction(`Bid raised to ${formatAmount.format(newAmount)}.`, async () => {
            await authRequest<HoldResponse>(
                `/api/wallets/holds/${activeHold.holdId}/release`,
                {method: "POST"},
            );
            const response = await authRequest<HoldResponse>(
                "/api/wallets/me/holds",
                {
                    method: "POST",
                    body: JSON.stringify({
                        amount: newAmount,
                    }),
                },
            );
            setActiveHold(response);
        });
    };

    const actionsDisabled = !session || syncing || acting;

    return (
        <main className="min-h-screen bg-[#F1E9D9] text-[#003060]">
            <section className="bg-gradient-to-r from-[#003060] to-[#00162D] py-10 text-center text-[#F1E9D9] shadow-md">
                <p className="text-sm uppercase tracking-[0.5em] opacity-80">
                    BidMart wallet simulator
                </p>
                <p className="mt-2 text-xl">{sessionMsg}</p>
                {!session && (
                    <p className="text-sm mt-2">
                        <Link className="underline" href="/login">
                            Login disini
                        </Link>
                    </p>
                )}
            </section>

            <section className="mx-auto w-full max-w-5xl px-6 py-10">
                <header className="mb-8 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                    <div>
                        <h1 className="text-3xl font-bold">
                            Wallet Backend Integrated Simulator
                        </h1>
                        <p className="text-[#003060]/80">
                            Semua aksi di bawah menggunakan akun Anda saat ini.
                        </p>
                    </div>
                    <button
                        onClick={() => syncWallet()}
                        className="rounded-lg border border-[#003060] px-4 py-2 text-sm font-semibold transition hover:bg-[#003060] hover:text-[#F1E9D9]"
                        disabled={!session || syncing}
                    >
                        Refresh balances
                    </button>
                </header>

                <div className="grid gap-6 md:grid-cols-2">
                    <div className="rounded-xl bg-white/80 p-6 shadow">
                        <h2 className="text-lg font-semibold">Balances</h2>
                        <div className="mt-4 space-y-3 font-mono">
                            <div className="rounded-lg bg-[#003060]/5 p-4">
                                <p className="text-sm opacity-80">Current Balance</p>
                                <p className="text-2xl font-semibold">
                                    {wallet
                                        ? formatAmount.format(wallet.availableBalance)
                                        : "—"}
                                </p>
                            </div>
                            <div className="rounded-lg bg-[#003060]/5 p-4">
                                <p className="text-sm opacity-80">Held Balance</p>
                                <p className="text-2xl font-semibold">
                                    {wallet
                                        ? formatAmount.format(wallet.heldBalance)
                                        : "—"}
                                </p>
                            </div>
                            <div className="rounded-lg bg-[#003060]/5 p-4">
                                <p className="text-sm opacity-80">Last Updated</p>
                                <p className="text-xl">
                                    {wallet?.updatedAt
                                        ? new Date(wallet.updatedAt).toLocaleString("id-ID")
                                        : "—"}
                                </p>
                            </div>
                            <div className="rounded-lg bg-[#003060]/5 p-4">
                                <p className="text-sm opacity-80">Active Hold</p>
                                <p className="break-all text-sm">
                                    {activeHold
                                        ? `${activeHold.holdId} (${activeHold.status})`
                                        : "No hold"}
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="rounded-xl bg-white/80 p-6 shadow">
                        <h2 className="text-lg font-semibold">Actions</h2>
                        <div className="mt-4 space-y-4">
                            <div className="flex flex-wrap gap-3">
                                <button
                                    className="rounded-lg bg-gradient-to-r from-[#003060] to-[#00162D] px-4 py-2 font-semibold text-[#F1E9D9] transition hover:opacity-90 disabled:opacity-60"
                                    onClick={() => syncWallet()}
                                    disabled={actionsDisabled}
                                >
                                    Create Wallet
                                </button>
                                <button
                                    className="rounded-lg border border-[#003060] px-4 py-2 font-semibold transition hover:bg-[#003060] hover:text-[#F1E9D9] disabled:opacity-60"
                                    onClick={handleReset}
                                    disabled={actionsDisabled}
                                >
                                    Recreate Wallet
                                </button>
                            </div>

                            <div>
                                <label className="text-sm text-[#003060]/80">
                                    Top Up Amount (IDR)
                                </label>
                                <div className="mt-2 flex gap-3">
                                    <input
                                        type="number"
                                        className="flex-1 rounded-lg border border-[#003060]/40 bg-[#F1E9D9] px-3 py-2 text-[#003060] focus:border-[#003060] focus:outline-none"
                                        value={topUpInput}
                                        onChange={(e) => setTopUpInput(e.target.value)}
                                        disabled={actionsDisabled}
                                    />
                                    <button
                                        className="rounded-lg bg-gradient-to-r from-[#003060] to-[#00162D] px-4 py-2 font-semibold text-[#F1E9D9] transition hover:opacity-90 disabled:opacity-60"
                                        onClick={handleTopUp}
                                        disabled={actionsDisabled}
                                    >
                                        Top Up Wallet
                                    </button>
                                </div>
                            </div>

                            <div>
                                <label className="text-sm text-[#003060]/80">
                                    Bid Amount (IDR)
                                </label>
                                <div className="mt-2 flex gap-3">
                                    <input
                                        type="number"
                                        className="flex-1 rounded-lg border border-[#003060]/40 bg-[#F1E9D9] px-3 py-2 text-[#003060] focus:border-[#003060] focus:outline-none"
                                        value={bidInput}
                                        onChange={(e) => setBidInput(e.target.value)}
                                        disabled={actionsDisabled}
                                    />
                                    <button
                                        className="rounded-lg bg-gradient-to-r from-[#003060] to-[#00162D] px-4 py-2 font-semibold text-[#F1E9D9] transition hover:opacity-90 disabled:opacity-60"
                                        onClick={() =>
                                            handleBid(
                                                Number(bidInput),
                                                "Bid placed via hold.",
                                            )
                                        }
                                        disabled={actionsDisabled}
                                    >
                                        Bid at Price
                                    </button>
                                </div>
                            </div>

                            <div>
                                <label className="text-sm text-[#003060]/80">
                                    Raise Amount (IDR)
                                </label>
                                <div className="mt-2 flex gap-3">
                                    <input
                                        type="number"
                                        className="flex-1 rounded-lg border border-[#003060]/40 bg-[#F1E9D9] px-3 py-2 text-[#003060] focus:border-[#003060] focus:outline-none"
                                        value={raiseInput}
                                        onChange={(e) => setRaiseInput(e.target.value)}
                                        disabled={actionsDisabled}
                                    />
                                    <button
                                        className="rounded-lg bg-gradient-to-r from-[#003060] to-[#00162D] px-4 py-2 font-semibold text-[#F1E9D9] transition hover:opacity-90 disabled:opacity-60"
                                        onClick={handleRaiseBid}
                                        disabled={actionsDisabled}
                                    >
                                        Raise Bid
                                    </button>
                                </div>
                            </div>

                            <button
                                className="w-full rounded-lg border border-[#003060] px-4 py-2 font-semibold transition hover:bg-[#003060] hover:text-[#F1E9D9] disabled:opacity-60"
                                onClick={handleReleaseHold}
                                disabled={actionsDisabled}
                            >
                                Release Bid
                            </button>
                        </div>
                    </div>
                </div>

                <div className="mt-8 rounded-xl bg-white/80 p-4 text-sm text-[#003060] shadow">
                    <p className="font-semibold">Status</p>
                    <p>{status}</p>
                    {syncing && (
                        <p className="text-xs text-[#003060]/70">
                            Talking to backend...
                        </p>
                    )}
                </div>
            </section>
        </main>
    );
}
