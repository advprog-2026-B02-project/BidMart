import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import WalletPage from "@/app/wallet/page";

jest.mock("next/link", () => {
    const Link = ({ children, href }: { children: React.ReactNode; href: string }) => (
        <a href={href}>{children}</a>
    );
    Link.displayName = "Link";
    return Link;
});

jest.mock("@/lib/api", () => ({
    me: jest.fn(),
}));

import { me } from "@/lib/api";
const mockMe = me as jest.Mock;

const mockWallet = {
    userId: "user-1",
    availableBalance: 500000,
    heldBalance: 50000,
    totalBalance: 550000,
    updatedAt: "2026-04-17T10:00:00",
};

const mockHold = {
    holdId: "hold-abc",
    userId: "user-1",
    amount: 50000,
    status: "ACTIVE",
    createdAt: "2026-04-17T10:00:00",
};

const mockTxPage0 = {
    content: [
        {
            id: "tx-1",
            type: "TOPUP",
            amount: 100000,
            description: "Top-up saldo",
            referenceId: null,
            balanceAfter: 100000,
            createdAt: "2026-04-17T09:00:00",
        },
        {
            id: "tx-2",
            type: "HOLD",
            amount: -50000,
            description: "Hold balance for bid",
            referenceId: "auction-1",
            balanceAfter: 50000,
            createdAt: "2026-04-17T09:05:00",
        },
    ],
    totalElements: 2,
    totalPages: 1,
    number: 0,
    size: 5,
};


const mockWithdrawResult = {
    transactionId: "tx-wd-1",
    amount: 100000,
    fee: 5000,
    netAmount: 95000,
    status: "PROCESSING",
    estimatedCompletion: "2026-04-18T10:00:00",
};

function makeFetch(handlers: Record<string, unknown>) {
    return jest.fn((url: string, _init?: RequestInit) => {
        const path = url.replace(/^https?:\/\/[^/]+/, "").split("?")[0];
        const sorted = Object.entries(handlers).sort((a, b) => b[0].length - a[0].length);
        for (const [key, val] of sorted) {
            if (path === key || path.startsWith(key + "/")) {
                if (val === null) {
                    return Promise.resolve({
                        ok: false,
                        status: 409,
                        statusText: "Conflict",
                        text: () => Promise.resolve("Saldo tidak mencukupi"),
                        json: () => Promise.reject(new Error("not json")),
                    });
                }
                return Promise.resolve({
                    ok: true,
                    status: 200,
                    json: () => Promise.resolve(val),
                    text: () => Promise.resolve(""),
                });
            }
        }
        return Promise.resolve({
            ok: true,
            status: 200,
            json: () => Promise.resolve({}),
            text: () => Promise.resolve(""),
        });
    });
}

function setupLoggedIn(fetchOverrides: Record<string, unknown> = {}) {
    mockMe.mockResolvedValue({ userId: "user-1", email: "test@example.com" });
    global.fetch = makeFetch({
        "/api/wallets/me/transactions": mockTxPage0,
        "/api/wallets/me/withdraw": mockWithdrawResult,
        "/api/wallets/me/top-up": mockWallet,
        "/api/wallets/holds": mockHold,
        "/api/wallets/me/reset": mockWallet,
        "/api/wallets/me": mockWallet,
        ...fetchOverrides,
    }) as jest.Mock;
    localStorage.setItem("accessToken", "test-token");
}

beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
});

afterEach(() => {
    jest.clearAllMocks();
});

describe("WalletPage – unauthenticated", () => {
    it("shows checking session initially then error when me() fails", async () => {
        mockMe.mockRejectedValue(new Error("Unauthorized"));
        global.fetch = jest.fn();
        render(<WalletPage />);
        expect(screen.getByText("Checking session...")).toBeInTheDocument();
        await waitFor(() =>
            expect(screen.getByText("Unauthorized")).toBeInTheDocument()
        );
    });

    it("shows login link when unauthenticated", async () => {
        mockMe.mockRejectedValue(new Error("Unauthorized"));
        global.fetch = jest.fn();
        render(<WalletPage />);
        await waitFor(() =>
            expect(screen.getByRole("link", { name: /login/i })).toBeInTheDocument()
        );
    });

    it("handles string error from me()", async () => {
        mockMe.mockRejectedValue("plain string error");
        global.fetch = jest.fn();
        render(<WalletPage />);
        await waitFor(() =>
            expect(screen.getByText("plain string error")).toBeInTheDocument()
        );
    });

    it("handles unknown error type from me()", async () => {
        mockMe.mockRejectedValue(42);
        global.fetch = jest.fn();
        render(<WalletPage />);
        await waitFor(() =>
            expect(screen.getByText("Unexpected error")).toBeInTheDocument()
        );
    });

    it("shows page counter as 1 when totalPages is zero", async () => {
        setupLoggedIn({
            "/api/wallets/me/transactions": {
                content: [
                    {
                        id: "tx-z",
                        type: "TOPUP",
                        amount: 1000,
                        description: "seed",
                        referenceId: null,
                        balanceAfter: 1000,
                        createdAt: "2026-04-17T09:00:00",
                    },
                ],
                totalElements: 1,
                totalPages: 0,
                number: 0,
                size: 5,
            },
        });
        render(<WalletPage />);
        await waitFor(() => screen.getByText("seed"));
        expect(screen.getByText(/1 \/ 1/)).toBeInTheDocument();
    });
});

describe("WalletPage – authenticated", () => {
    it("shows logged-in email after successful session", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() =>
            expect(screen.getByText("Logged in as test@example.com")).toBeInTheDocument()
        );
    });

    it("syncs wallet and shows status after session loads", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() =>
            expect(screen.getByText("Wallet synced with backend.")).toBeInTheDocument()
        );
    });

    it("renders wallet balances panel", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() =>
            expect(screen.getByText("Current Balance")).toBeInTheDocument()
        );
        expect(screen.getByText("Held Balance")).toBeInTheDocument();
    });

    it("renders transaction table when transactions exist", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() =>
            expect(screen.getByText("Top-up saldo")).toBeInTheDocument()
        );
        expect(screen.getByText("Hold balance for bid")).toBeInTheDocument();
    });

    it("shows empty state when no transactions", async () => {
        setupLoggedIn({
            "/api/wallets/me/transactions": {
                content: [],
                totalElements: 0,
                totalPages: 0,
                number: 0,
                size: 5,
            },
        });
        render(<WalletPage />);
        await waitFor(() =>
            expect(screen.getByText("Belum ada transaksi.")).toBeInTheDocument()
        );
    });

    it("clicks Create Wallet button to sync wallet", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        const countBefore = (global.fetch as jest.Mock).mock.calls.filter(
            (c: unknown[]) => (c[0] as string).includes("/api/wallets/me") &&
                !(c[0] as string).includes("transactions")
        ).length;
        fireEvent.click(screen.getByRole("button", { name: "Create Wallet" }));
        await waitFor(() => {
            const countAfter = (global.fetch as jest.Mock).mock.calls.filter(
                (c: unknown[]) => (c[0] as string).includes("/api/wallets/me") &&
                    !(c[0] as string).includes("transactions")
            ).length;
            expect(countAfter).toBeGreaterThan(countBefore);
        });
    });

    it("prev page button click fetches previous page", async () => {
        setupLoggedIn({
            "/api/wallets/me/transactions": {
                ...mockTxPage0,
                number: 1,
                totalPages: 3,
            },
        });
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Top-up saldo"));
        const prev = screen.getByRole("button", { name: /← prev/i });
        expect(prev).not.toBeDisabled();
        fireEvent.click(prev);
        await waitFor(() => {
            const calls = (global.fetch as jest.Mock).mock.calls;
            expect(calls.some((c: unknown[]) => (c[0] as string).includes("page=0"))).toBe(true);
        });
    });

    it("renders transaction type label from map (Top Up)", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() =>
            expect(screen.getByText("Top Up")).toBeInTheDocument()
        );
    });

    it("renders transaction type fallback when type not in map", async () => {
        setupLoggedIn({
            "/api/wallets/me/transactions": {
                ...mockTxPage0,
                content: [{
                    id: "tx-x",
                    type: "UNKNOWN_TYPE",
                    amount: 10000,
                    description: "mystery",
                    referenceId: null,
                    balanceAfter: 10000,
                    createdAt: "2026-04-17T09:00:00",
                }],
            },
        });
        render(<WalletPage />);
        await waitFor(() =>
            expect(screen.getByText("UNKNOWN_TYPE")).toBeInTheDocument()
        );
    });

    it("shows no hold initially", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() =>
            expect(screen.getByText("No hold")).toBeInTheDocument()
        );
    });

    it("shows pagination info when multiple pages exist", async () => {
        setupLoggedIn({
            "/api/wallets/me/transactions": {
                ...mockTxPage0,
                totalElements: 6,
                totalPages: 3,
                number: 0,
            },
        });
        render(<WalletPage />);
        await waitFor(() =>
            expect(screen.getByText(/3/)).toBeInTheDocument()
        );
        expect(screen.getByText(/6 transaksi total/)).toBeInTheDocument();
    });

    it("prev button disabled on page 0", async () => {
        setupLoggedIn({
            "/api/wallets/me/transactions": {
                ...mockTxPage0,
                totalPages: 3,
                number: 0,
            },
        });
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Top-up saldo"));
        const prev = screen.getByRole("button", { name: /← prev/i });
        expect(prev).toBeDisabled();
    });

    it("next button disabled on last page", async () => {
        setupLoggedIn({
            "/api/wallets/me/transactions": {
                ...mockTxPage0,
                totalPages: 1,
                number: 0,
            },
        });
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Top-up saldo"));
        const next = screen.getByRole("button", { name: /next →/i });
        expect(next).toBeDisabled();
    });

    it("next page button click fetches page 1", async () => {
        const fetchMock = makeFetch({
            "/api/wallets/me/transactions": {
                ...mockTxPage0,
                totalElements: 6,
                totalPages: 3,
                number: 0,
            },
            "/api/wallets/me": mockWallet,
        }) as jest.Mock;
        mockMe.mockResolvedValue({ userId: "user-1", email: "test@example.com" });
        global.fetch = fetchMock;
        localStorage.setItem("accessToken", "test-token");
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Top-up saldo"));
        const next = screen.getByRole("button", { name: /next →/i });
        fireEvent.click(next);
        await waitFor(() => {
            const calls = (global.fetch as jest.Mock).mock.calls;
            expect(calls.some((c: unknown[]) => (c[0] as string).includes("page=1"))).toBe(true);
        });
    });

    it("refresh transactions button calls fetchTransactions", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Top-up saldo"));
        const countBefore = (global.fetch as jest.Mock).mock.calls.filter(
            (c: unknown[]) => (c[0] as string).includes("transactions")
        ).length;
        fireEvent.click(screen.getByRole("button", { name: /^refresh$/i }));
        await waitFor(() => {
            const countAfter = (global.fetch as jest.Mock).mock.calls.filter(
                (c: unknown[]) => (c[0] as string).includes("transactions")
            ).length;
            expect(countAfter).toBeGreaterThan(countBefore);
        });
    });

    it("refresh balances button syncs wallet again", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        const btn = screen.getByRole("button", { name: /refresh balances/i });
        fireEvent.click(btn);
        await waitFor(() => {
            expect(
                (global.fetch as jest.Mock).mock.calls.filter(
                    (c: unknown[]) => (c[0] as string).includes("/api/wallets/me") &&
                        !(c[0] as string).includes("transactions")
                ).length
            ).toBeGreaterThan(1);
        });
    });

    it("wallet sync error is shown in status", async () => {
        mockMe.mockResolvedValue({ userId: "user-1", email: "test@example.com" });
        localStorage.setItem("accessToken", "test-token");
        global.fetch = jest.fn((url: string) => {
            const path = url.replace(/^https?:\/\/[^/]+/, "");
            if (path.includes("transactions")) {
                return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(mockTxPage0), text: () => Promise.resolve("") });
            }
            return Promise.resolve({ ok: false, status: 500, statusText: "Server error", text: () => Promise.resolve(""), json: () => Promise.reject(new Error("")) });
        }) as jest.Mock;
        render(<WalletPage />);
        await waitFor(() =>
            expect(screen.getByText("Server error")).toBeInTheDocument()
        );
    });

    it("fetchTransactions error is silently swallowed", async () => {
        mockMe.mockResolvedValue({ userId: "user-1", email: "test@example.com" });
        localStorage.setItem("accessToken", "test-token");
        global.fetch = jest.fn((url: string) => {
            const path = url.replace(/^https?:\/\/[^/]+/, "");
            if (path.includes("transactions")) {
                return Promise.resolve({ ok: false, status: 500, statusText: "fail", text: () => Promise.resolve("tx error"), json: () => Promise.reject(new Error("")) });
            }
            return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(mockWallet), text: () => Promise.resolve("") });
        }) as jest.Mock;
        render(<WalletPage />);
        await waitFor(() =>
            expect(screen.getByText("Wallet synced with backend.")).toBeInTheDocument()
        );
        expect(screen.queryByText("tx error")).not.toBeInTheDocument();
    });

    it("authRequest works without token (no Authorization header)", async () => {
        mockMe.mockResolvedValue({ userId: "user-1", email: "test@example.com" });
        global.fetch = makeFetch({
            "/api/wallets/me/transactions": mockTxPage0,
            "/api/wallets/me": mockWallet,
        }) as jest.Mock;
        render(<WalletPage />);
        await waitFor(() =>
            expect(screen.getByText("Wallet synced with backend.")).toBeInTheDocument()
        );
    });
});

describe("WalletPage – top up handler", () => {
    it("shows error for invalid (zero) top up amount", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        const inputs = screen.getAllByRole("spinbutton");
        fireEvent.change(inputs[0], { target: { value: "0" } });
        fireEvent.click(screen.getByRole("button", { name: /top up$/i }));
        expect(screen.getByText("Top up amount must be positive.")).toBeInTheDocument();
    });

    it("shows error for negative top up amount", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        const inputs = screen.getAllByRole("spinbutton");
        fireEvent.change(inputs[0], { target: { value: "-500" } });
        fireEvent.click(screen.getByRole("button", { name: /top up$/i }));
        expect(screen.getByText("Top up amount must be positive.")).toBeInTheDocument();
    });

    it("successful top up shows success message", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        fireEvent.click(screen.getByRole("button", { name: /top up$/i }));
        await waitFor(() =>
            expect(screen.getByText(/Top up successful/i)).toBeInTheDocument()
        );
    });

    it("top up API error shows error in status", async () => {
        setupLoggedIn({ "/api/wallets/me/top-up": null });
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        fireEvent.click(screen.getByRole("button", { name: /top up$/i }));
        await waitFor(() =>
            expect(screen.getByText("Saldo tidak mencukupi")).toBeInTheDocument()
        );
    });
});

describe("WalletPage – bid handler", () => {
    it("shows error for invalid bid amount", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        const inputs = screen.getAllByRole("spinbutton");
        fireEvent.change(inputs[1], { target: { value: "0" } });
        fireEvent.click(screen.getByRole("button", { name: /bid at price/i }));
        expect(screen.getByText("Bid amount must be positive.")).toBeInTheDocument();
    });

    it("successful bid shows success message and sets hold", async () => {
        setupLoggedIn({ "/api/wallets/me/holds": mockHold });
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        fireEvent.click(screen.getByRole("button", { name: /bid at price/i }));
        await waitFor(() =>
            expect(screen.getByText(/hold-abc/i)).toBeInTheDocument()
        );
    });
});

describe("WalletPage – release hold handler", () => {
    it("shows error when no active hold to release", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        fireEvent.click(screen.getByRole("button", { name: /release bid/i }));
        expect(screen.getByText("No active hold to release.")).toBeInTheDocument();
    });

    it("release hold succeeds after bid placed", async () => {
        setupLoggedIn({
            "/api/wallets/me/holds": mockHold,
            "/api/wallets/holds": mockHold,
        });
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        fireEvent.click(screen.getByRole("button", { name: /bid at price/i }));
        await waitFor(() => screen.getByText(/hold-abc/i));
        fireEvent.click(screen.getByRole("button", { name: /release bid/i }));
        await waitFor(() =>
            expect(screen.getByText("Hold released.")).toBeInTheDocument()
        );
    });
});

describe("WalletPage – raise bid handler", () => {
    it("shows error when no active hold for raise", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        fireEvent.click(screen.getByRole("button", { name: /raise bid/i }));
        expect(screen.getByText("Place a bid before raising.")).toBeInTheDocument();
    });

    it("shows error for invalid raise increment", async () => {
        setupLoggedIn({ "/api/wallets/me/holds": mockHold });
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        fireEvent.click(screen.getByRole("button", { name: /bid at price/i }));
        await waitFor(() => screen.getByText(/hold-abc/i));
        const inputs = screen.getAllByRole("spinbutton");
        fireEvent.change(inputs[2], { target: { value: "0" } });
        fireEvent.click(screen.getByRole("button", { name: /raise bid/i }));
        expect(screen.getByText("Raise amount must be positive.")).toBeInTheDocument();
    });

    it("successful raise bid shows raised status", async () => {
        const raisedHold = { ...mockHold, holdId: "hold-raised", amount: 60000 };
        setupLoggedIn({
            "/api/wallets/me/holds": raisedHold,
            "/api/wallets/holds": mockHold,
        });
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        fireEvent.click(screen.getByRole("button", { name: /bid at price/i }));
        await waitFor(() => screen.getByText(/hold-raised/i));
        fireEvent.click(screen.getByRole("button", { name: /raise bid/i }));
        await waitFor(() =>
            expect(screen.getByText(/Bid raised to/i)).toBeInTheDocument()
        );
    });
});

describe("WalletPage – reset handler", () => {
    it("reset shows success message", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        fireEvent.click(screen.getByRole("button", { name: /recreate wallet/i }));
        await waitFor(() =>
            expect(screen.getByText("Wallet reset to zero.")).toBeInTheDocument()
        );
    });
});

describe("WalletPage – withdraw handler", () => {
    it("shows error for amount below minimum", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        const inputs = screen.getAllByRole("spinbutton");
        const withdrawInput = inputs[inputs.length - 1];
        fireEvent.change(withdrawInput, { target: { value: "5000" } });
        fireEvent.click(screen.getByRole("button", { name: /tarik dana/i }));
        expect(screen.getByText("Minimum withdrawal is Rp 10.000.")).toBeInTheDocument();
    });

    it("shows error when bank code is empty", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        fireEvent.click(screen.getByRole("button", { name: /tarik dana/i }));
        expect(screen.getByText("Lengkapi data rekening terlebih dahulu.")).toBeInTheDocument();
    });

    it("shows error when account number is empty", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        fireEvent.change(screen.getByPlaceholderText("BCA"), { target: { value: "BCA" } });
        fireEvent.click(screen.getByRole("button", { name: /tarik dana/i }));
        expect(screen.getByText("Lengkapi data rekening terlebih dahulu.")).toBeInTheDocument();
    });

    it("shows error when account name is empty", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        fireEvent.change(screen.getByPlaceholderText("BCA"), { target: { value: "BCA" } });
        fireEvent.change(screen.getByPlaceholderText("1234567890"), { target: { value: "1234567890" } });
        fireEvent.click(screen.getByRole("button", { name: /tarik dana/i }));
        expect(screen.getByText("Lengkapi data rekening terlebih dahulu.")).toBeInTheDocument();
    });

    it("successful withdrawal shows confirmation card", async () => {
        setupLoggedIn();
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        fireEvent.change(screen.getByPlaceholderText("BCA"), { target: { value: "BCA" } });
        fireEvent.change(screen.getByPlaceholderText("1234567890"), { target: { value: "1234567890" } });
        fireEvent.change(screen.getByPlaceholderText("John Doe"), { target: { value: "John Doe" } });
        fireEvent.click(screen.getByRole("button", { name: /tarik dana/i }));
        await waitFor(() =>
            expect(screen.getByText("Konfirmasi Penarikan")).toBeInTheDocument()
        );
        expect(screen.getByText("PROCESSING")).toBeInTheDocument();
    });

    it("withdrawal API error shows error in status", async () => {
        setupLoggedIn({ "/api/wallets/me/withdraw": null });
        render(<WalletPage />);
        await waitFor(() => screen.getByText("Wallet synced with backend."));
        fireEvent.change(screen.getByPlaceholderText("BCA"), { target: { value: "BCA" } });
        fireEvent.change(screen.getByPlaceholderText("1234567890"), { target: { value: "1234567890" } });
        fireEvent.change(screen.getByPlaceholderText("John Doe"), { target: { value: "John Doe" } });
        fireEvent.click(screen.getByRole("button", { name: /tarik dana/i }));
        await waitFor(() =>
            expect(screen.getByText("Saldo tidak mencukupi")).toBeInTheDocument()
        );
    });
});
