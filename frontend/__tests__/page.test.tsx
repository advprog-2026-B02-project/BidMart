import {render, screen, waitFor} from "@testing-library/react";
import Home from "@/app/page";
import "@testing-library/jest-dom";

global.fetch = jest.fn(() =>
    Promise.resolve({
        json: () => Promise.resolve(10),
    }),
) as jest.Mock;

describe("Home Page", () => {
    it("renders counter value from api", async () => {
        render(<Home />);
        await waitFor(() => {
            expect(screen.getByText((content, element) =>
                element?.tagName === "H1" && content.includes("Views:"),
            )).toBeInTheDocument();
            expect(screen.getByText("10")).toBeInTheDocument();
        });
    });
});
