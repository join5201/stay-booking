import { setupServer } from "msw/node";
import { handlers } from "./handlers";

// Vitest용 MSW 서버. 테스트 파일이 beforeAll listen, afterEach resetHandlers, afterAll close
export const server = setupServer(...handlers);
