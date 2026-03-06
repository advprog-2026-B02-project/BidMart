"use client";

import {useCallback, useEffect, useMemo, useState} from "react";

const BACKEND_URL =
    process.env.NEXT_PUBLIC_BACKEND_URL ?? "http://localhost:8080";
const DEMO_USER_ID = "11111111-1111-1111-1111-111111111111";
const DEMO_AUCTION_ID = "22222222-2222-2222-2222-222222222222"; 

const randomId = () =>
    typeof crypto !== "undefined" && "randomUUID" in crypto
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random().toString(16).slice(2)}`;

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

const request = async <T,>(path: string, init?: RequestInit): Promise<T> => {
    const headers = init?.body
        ? {
              "Content-Type": "application/json",
              ...(init.headers ?? {}),
          }
        : init?.headers;

    const response = await fetch(`${BACKEND_URL}${path}`, {
        cache: "no-store",
        ...init,
        headers,
    });
    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || response.statusText);
    }
    return (await response.json()) as T;
};

export default function WalletPage() {
    const [views, setViews] = useState(0);
    const [wallet, setWallet] = useState<WalletResponse | null>(null);
    const [activeHold, setActiveHold] = useState<HoldResponse | null>(null);
    const [topUpInput, setTopUpInput] = useState("100000");
    const [bidInput, setBidInput] = useState("50000");
    const [raiseInput, setRaiseInput] = useState("10000");
    const [status, setStatus] = useState("Wallet not created yet.");
    const [syncing, setSyncing] = useState(false);
    const [acting, setActing] = useState(false);

    useEffect(() => {
        fetch(`${BACKEND_URL}/api/counter`)
            .then((res) => res.json())
            .then((data) => setViews(data))
            .catch(() => {
            });
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
        setSyncing(true);
        try {
            const data = await request<WalletResponse>(
                `/api/wallets/${DEMO_USER_ID}`,
            );
            setWallet(data);
            setStatus("Wallet synced with backend.");
        } catch (error) {
            setStatus(parseError(error));
        } finally {
            setSyncing(false);
        }
    }, []);

    useEffect(() => {
        syncWallet();
    }, [syncWallet]);

    const runAction = useCallback(
        async (message: string, action: () => Promise<unknown>) => {
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
        [syncWallet],
    );

    const handleTopUp = () => {
        const amount = Number(topUpInput);
        if (!Number.isFinite(amount) || amount <= 0) {
            setStatus("Top up amount must be positive.");
            return;
        }
        runAction(`Top up successful (+${formatAmount.format(amount)}).`, () =>
            request(`/api/wallets/${DEMO_USER_ID}/top-up`, {
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
            const response = await request<HoldResponse>(
                `/api/wallets/${DEMO_USER_ID}/holds`,
                {
                    method: "POST",
                    body: JSON.stringify({
                        userId: DEMO_USER_ID,
                        auctionId: DEMO_AUCTION_ID,
                        bidId: randomId(),
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
            await request<HoldResponse>(
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
            await request(`/api/wallets/${DEMO_USER_ID}/reset`, {
                method: "POST",
            });
            setActiveHold(null);
        });
    };

    const isBusy = syncing || acting;

    return (
        <main className="min-h-screen bg-[#F1E9D9] text-[#003060]">
            <section className="bg-gradient-to-r from-[#003060] to-[#00162D] py-10 text-center text-[#F1E9D9] shadow-md">
                <p className="text-sm uppercase tracking-[0.5em] opacity-80">
                    BidMart internal metrics
                </p>
                <p className="mt-2 text-4xl font-mono font-semibold">
                    Views: <span>{views}</span>
                </p>
                <p className="text-xs opacity-70">Demo user: {DEMO_USER_ID}</p>
            </section>

            <section className="mx-auto w-full max-w-5xl px-6 py-10">
                <header className="mb-8 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                    <div>
                        <h1 className="text-3xl font-bold">
                            Wallet Backend Integrated Simulator
                        </h1>
                    </div>
                    <button
                        onClick={syncWallet}
                        className="rounded-lg border border-[#003060] px-4 py-2 text-sm font-semibold transition hover:bg-[#003060] hover:text-[#F1E9D9]"
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
                                        ? new Date(wallet.updatedAt).toLocaleString(
                                              "id-ID",
                                          )
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
                                    onClick={syncWallet}
                                    disabled={isBusy}
                                >
                                    Create Wallet
                                </button>
                                <button
                                    className="rounded-lg border border-[#003060] px-4 py-2 font-semibold transition hover:bg-[#003060] hover:text-[#F1E9D9] disabled:opacity-60"
                                    onClick={handleReset}
                                    disabled={isBusy}
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
                                        disabled={isBusy}
                                    />
                                    <button
                                        className="rounded-lg bg-gradient-to-r from-[#003060] to-[#00162D] px-4 py-2 font-semibold text-[#F1E9D9] transition hover:opacity-90 disabled:opacity-60"
                                        onClick={handleTopUp}
                                        disabled={isBusy}
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
                                        disabled={isBusy}
                                    />
                                    <button
                                        className="rounded-lg bg-gradient-to-r from-[#003060] to-[#00162D] px-4 py-2 font-semibold text-[#F1E9D9] transition hover:opacity-90 disabled:opacity-60"
                                        onClick={() =>
                                            handleBid(
                                                Number(bidInput),
                                                "Bid placed via hold.",
                                            )
                                        }
                                        disabled={isBusy}
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
                                        disabled={isBusy}
                                    />
                                    <button
                                        className="rounded-lg bg-gradient-to-r from-[#003060] to-[#00162D] px-4 py-2 font-semibold text-[#F1E9D9] transition hover:opacity-90 disabled:opacity-60"
                                        onClick={() =>
                                            handleBid(
                                                Number(raiseInput) +
                                                    (activeHold?.amount ?? 0),
                                                "Bid raised.",
                                            )
                                        }
                                        disabled={isBusy}
                                    >
                                        Raise Bid
                                    </button>
                                </div>
                            </div>

                            <button
                                className="w-full rounded-lg border border-[#003060] px-4 py-2 font-semibold transition hover:bg-[#003060] hover:text-[#F1E9D9] disabled:opacity-60"
                                onClick={handleReleaseHold}
                                disabled={isBusy}
                            >
                                Release Bid
                            </button>
                        </div>
                    </div>
                </div>

                <div className="mt-8 rounded-xl bg-white/80 p-4 text-sm text-[#003060] shadow">
                    <p className="font-semibold">Status</p>
                    <p>{status}</p>
                    {isBusy && (
                        <p className="text-xs text-[#003060]/70">
                            Talking to backend...
                        </p>
                    )}
                </div>
            </section>
        </main>
    );
}
