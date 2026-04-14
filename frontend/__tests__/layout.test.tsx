import {renderToStaticMarkup} from "react-dom/server";
import RootLayout, {metadata} from "../app/layout";

// stupidest unit test ever JUST to get 100% COVERAGE

jest.mock("next/font/google", () => ({
    Plus_Jakarta_Sans: () => ({variable: "--font-plus-jakarta"}),
}));

describe("RootLayout Statements", () => {
    it("executes all statements including metadata and layout", () => {
        expect(metadata.title).toBe("BidMart");
        expect(metadata.description).toBe("BidMart Auth");

        const markup = renderToStaticMarkup(
            <RootLayout>
                <div data-testid="child">Check Statements</div>
            </RootLayout>,
        );

        expect(markup).toContain("Check Statements");
    });
});
