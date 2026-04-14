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

type TransactionResponse = {
    id: string;
    type: string;
    amount: number;
    description: string;
    referenceId?: string | null;
    balanceAfter: number;
    createdAt: string;
};

type PageResponse<T> = {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
};

type WithdrawResponse = {
    transactionId: string;
    amount: number;
    fee: number;
    netAmount: number;
    status: string;
    estimatedCompletion: string;
};

const TRANSACTION_TYPE_LABELS: Record<string, string> = {
    TOPUP: "Top Up",
    WITHDRAW: "Penarikan",
    HOLD: "Hold Bid",
    RELEASE: "Release Hold",
    CAPTURE: "Pembayaran",
    PAYMENT_RECEIVED: "Dana Diterima",
};

const parseError = (error: unknown) => {
    if (error instanceof Error) return error.message;
    if (typeof error === "string") return error;
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

    // Withdraw state
    const [withdrawAmount, setWithdrawAmount] = useState("100000");
    const [withdrawBank, setWithdrawBank] = useState("");
    const [withdrawAccount, setWithdrawAccount] = useState("");
    const [withdrawName, setWithdrawName] = useState("");
    const [withdrawResult, setWithdrawResult] = useState<WithdrawResponse | null>(null);

    // Transaction history state
    const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
    const [txPage, setTxPage] = useState(0);
    const [txTotalPages, setTxTotalPages] = useState(0);
    const [txTotalElements, setTxTotalElements] = useState(0);
    const [txLoading, setTxLoading] = useState(false);

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
        if (!session) return;
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

    const fetchTransactions = useCallback(async (page: number) => {
        if (!session) return;
        setTxLoading(true);
        try {
            const data = await authRequest<PageResponse<TransactionResponse>>(
                `/api/wallets/me/transactions?page=${page}&size=5&sort=createdAt,desc`,
            );
            setTransactions(data.content);
            setTxPage(data.number);
            setTxTotalPages(data.totalPages);
            setTxTotalElements(data.totalElements);
        } catch {
            // silently fail — transactions section shows empty state
        } finally {
            setTxLoading(false);
        }
    }, [session]);

    useEffect(() => {
        if (session) {
            syncWallet();
            fetchTransactions(0);
        }
    }, [session, syncWallet, fetchTransactions]);

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
                await fetchTransactions(0);
                setStatus(message);
            } catch (error) {
                setStatus(parseError(error));
            } finally {
                setActing(false);
            }
        },
        [session, syncWallet, fetchTransactions],
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
                    body: JSON.stringify({amount}),
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
                {method: "POST"},
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
                    body: JSON.stringify({amount: newAmount}),
                },
            );
            setActiveHold(response);
        });
    };

    const handleWithdraw = () => {
        const amount = Number(withdrawAmount);
        if (!Number.isFinite(amount) || amount < 10000) {
            setStatus("Minimum withdrawal is Rp 10.000.");
            return;
        }
        if (!withdrawBank.trim() || !withdrawAccount.trim() || !withdrawName.trim()) {
            setStatus("Lengkapi data rekening terlebih dahulu.");
            return;
        }
        setWithdrawResult(null);
        runAction("Withdrawal submitted.", async () => {
            const result = await authRequest<WithdrawResponse>("/api/wallets/me/withdraw", {
                method: "POST",
                body: JSON.stringify({
                    amount,
                    bankCode: withdrawBank.trim(),
                    accountNumber: withdrawAccount.trim(),
                    accountName: withdrawName.trim(),
                }),
            });
            setWithdrawResult(result);
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

                {/* Balance + Actions */}
                <div className="grid gap-6 md:grid-cols-2">
                    <div className="rounded-xl bg-white/80 p-6 shadow">
                        <h2 className="text-lg font-semibold">Balances</h2>
                        <div className="mt-4 space-y-3 font-mono">
                            <div className="rounded-lg bg-[#003060]/5 p-4">
                                <p className="text-sm opacity-80">Current Balance</p>
                                <p className="text-2xl font-semibold">
                                    {wallet ? formatAmount.format(wallet.availableBalance) : "—"}
                                </p>
                            </div>
                            <div className="rounded-lg bg-[#003060]/5 p-4">
                                <p className="text-sm opacity-80">Held Balance</p>
                                <p className="text-2xl font-semibold">
                                    {wallet ? formatAmount.format(wallet.heldBalance) : "—"}
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
                                <label className="text-sm text-[#003060]/80">Top Up Amount (IDR)</label>
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
                                        Top Up
                                    </button>
                                </div>
                            </div>

                            <div>
                                <label className="text-sm text-[#003060]/80">Bid Amount (IDR)</label>
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
                                        onClick={() => handleBid(Number(bidInput), "Bid placed via hold.")}
                                        disabled={actionsDisabled}
                                    >
                                        Bid at Price
                                    </button>
                                </div>
                            </div>

                            <div>
                                <label className="text-sm text-[#003060]/80">Raise Amount (IDR)</label>
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

                {/* Withdraw */}
                <div className="mt-6 rounded-xl bg-white/80 p-6 shadow">
                    <h2 className="text-lg font-semibold">Penarikan Dana</h2>
                    <div className="mt-4 grid gap-4 md:grid-cols-2">
                        <div className="space-y-3">
                            <div>
                                <label className="text-sm text-[#003060]/80">Jumlah (min. Rp 10.000)</label>
                                <input
                                    type="number"
                                    className="mt-1 w-full rounded-lg border border-[#003060]/40 bg-[#F1E9D9] px-3 py-2 text-[#003060] focus:border-[#003060] focus:outline-none"
                                    value={withdrawAmount}
                                    onChange={(e) => setWithdrawAmount(e.target.value)}
                                    disabled={actionsDisabled}
                                />
                            </div>
                            <div>
                                <label className="text-sm text-[#003060]/80">Kode Bank (contoh: BCA, BNI)</label>
                                <input
                                    type="text"
                                    className="mt-1 w-full rounded-lg border border-[#003060]/40 bg-[#F1E9D9] px-3 py-2 text-[#003060] focus:border-[#003060] focus:outline-none"
                                    value={withdrawBank}
                                    onChange={(e) => setWithdrawBank(e.target.value)}
                                    disabled={actionsDisabled}
                                    placeholder="BCA"
                                />
                            </div>
                            <div>
                                <label className="text-sm text-[#003060]/80">Nomor Rekening</label>
                                <input
                                    type="text"
                                    className="mt-1 w-full rounded-lg border border-[#003060]/40 bg-[#F1E9D9] px-3 py-2 text-[#003060] focus:border-[#003060] focus:outline-none"
                                    value={withdrawAccount}
                                    onChange={(e) => setWithdrawAccount(e.target.value)}
                                    disabled={actionsDisabled}
                                    placeholder="1234567890"
                                />
                            </div>
                            <div>
                                <label className="text-sm text-[#003060]/80">Nama Pemilik Rekening</label>
                                <input
                                    type="text"
                                    className="mt-1 w-full rounded-lg border border-[#003060]/40 bg-[#F1E9D9] px-3 py-2 text-[#003060] focus:border-[#003060] focus:outline-none"
                                    value={withdrawName}
                                    onChange={(e) => setWithdrawName(e.target.value)}
                                    disabled={actionsDisabled}
                                    placeholder="John Doe"
                                />
                            </div>
                            <button
                                className="w-full rounded-lg bg-gradient-to-r from-[#003060] to-[#00162D] px-4 py-2 font-semibold text-[#F1E9D9] transition hover:opacity-90 disabled:opacity-60"
                                onClick={handleWithdraw}
                                disabled={actionsDisabled}
                            >
                                Tarik Dana
                            </button>
                        </div>

                        {withdrawResult && (
                            <div className="rounded-lg bg-[#003060]/5 p-4 space-y-2 font-mono text-sm">
                                <p className="font-semibold text-base font-sans">Konfirmasi Penarikan</p>
                                <div className="flex justify-between">
                                    <span className="opacity-70">Jumlah</span>
                                    <span>{formatAmount.format(withdrawResult.amount)}</span>
                                </div>
                                <div className="flex justify-between">
                                    <span className="opacity-70">Biaya Admin</span>
                                    <span>- {formatAmount.format(withdrawResult.fee)}</span>
                                </div>
                                <div className="flex justify-between border-t border-[#003060]/20 pt-2 font-bold">
                                    <span>Diterima</span>
                                    <span>{formatAmount.format(withdrawResult.netAmount)}</span>
                                </div>
                                <div className="flex justify-between">
                                    <span className="opacity-70">Status</span>
                                    <span className="rounded-full bg-yellow-100 px-2 py-0.5 text-yellow-800 text-xs">
                                        {withdrawResult.status}
                                    </span>
                                </div>
                                <div className="flex justify-between">
                                    <span className="opacity-70">Estimasi Selesai</span>
                                    <span>
                                        {new Date(withdrawResult.estimatedCompletion).toLocaleDateString("id-ID")}
                                    </span>
                                </div>
                            </div>
                        )}
                    </div>
                </div>

                {/* Status */}
                <div className="mt-6 rounded-xl bg-white/80 p-4 text-sm text-[#003060] shadow">
                    <p className="font-semibold">Status</p>
                    <p>{status}</p>
                    {syncing && (
                        <p className="text-xs text-[#003060]/70">Talking to backend...</p>
                    )}
                </div>

                {/* Transaction History */}
                <div className="mt-6 rounded-xl bg-white/80 p-6 shadow">
                    <div className="flex items-center justify-between">
                        <h2 className="text-lg font-semibold">Riwayat Transaksi</h2>
                        <button
                            onClick={() => fetchTransactions(0)}
                            className="text-sm text-[#003060]/70 underline hover:text-[#003060]"
                            disabled={txLoading || !session}
                        >
                            Refresh
                        </button>
                    </div>

                    {txLoading ? (
                        <p className="mt-4 text-sm text-[#003060]/60">Memuat transaksi...</p>
                    ) : transactions.length === 0 ? (
                        <p className="mt-4 text-sm text-[#003060]/60">Belum ada transaksi.</p>
                    ) : (
                        <>
                            <div className="mt-4 overflow-x-auto">
                                <table className="w-full text-sm">
                                    <thead>
                                        <tr className="border-b border-[#003060]/10 text-left text-[#003060]/60">
                                            <th className="pb-2 pr-4 font-medium">Waktu</th>
                                            <th className="pb-2 pr-4 font-medium">Tipe</th>
                                            <th className="pb-2 pr-4 font-medium">Keterangan</th>
                                            <th className="pb-2 pr-4 font-medium text-right">Jumlah</th>
                                            <th className="pb-2 font-medium text-right">Saldo</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {transactions.map((txn) => (
                                            <tr
                                                key={txn.id}
                                                className="border-b border-[#003060]/5 hover:bg-[#003060]/5"
                                            >
                                                <td className="py-2 pr-4 text-[#003060]/60 whitespace-nowrap">
                                                    {new Date(txn.createdAt).toLocaleString("id-ID", {
                                                        day: "2-digit",
                                                        month: "short",
                                                        hour: "2-digit",
                                                        minute: "2-digit",
                                                    })}
                                                </td>
                                                <td className="py-2 pr-4">
                                                    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                                                        txn.amount >= 0
                                                            ? "bg-green-100 text-green-800"
                                                            : "bg-red-100 text-red-800"
                                                    }`}>
                                                        {TRANSACTION_TYPE_LABELS[txn.type] ?? txn.type}
                                                    </span>
                                                </td>
                                                <td className="py-2 pr-4 text-[#003060]/80 max-w-[200px] truncate">
                                                    {txn.description}
                                                </td>
                                                <td className={`py-2 pr-4 text-right font-mono font-medium ${
                                                    txn.amount >= 0 ? "text-green-700" : "text-red-700"
                                                }`}>
                                                    {txn.amount >= 0 ? "+" : ""}
                                                    {formatAmount.format(txn.amount)}
                                                </td>
                                                <td className="py-2 text-right font-mono text-[#003060]/70">
                                                    {formatAmount.format(txn.balanceAfter)}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>

                            <div className="mt-4 flex items-center justify-between text-sm text-[#003060]/60">
                                <span>{txTotalElements} transaksi total</span>
                                <div className="flex items-center gap-3">
                                    <button
                                        onClick={() => fetchTransactions(txPage - 1)}
                                        disabled={txPage === 0 || txLoading}
                                        className="rounded border border-[#003060]/30 px-3 py-1 hover:bg-[#003060]/10 disabled:opacity-40"
                                    >
                                        ← Prev
                                    </button>
                                    <span>
                                        {txPage + 1} / {txTotalPages || 1}
                                    </span>
                                    <button
                                        onClick={() => fetchTransactions(txPage + 1)}
                                        disabled={txPage >= txTotalPages - 1 || txLoading}
                                        className="rounded border border-[#003060]/30 px-3 py-1 hover:bg-[#003060]/10 disabled:opacity-40"
                                    >
                                        Next →
                                    </button>
                                </div>
                            </div>
                        </>
                    )}
                </div>
            </section>
        </main>
    );
}
