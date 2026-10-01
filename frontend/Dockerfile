# 프론트 이미지 (이슈 230)
#
# 왜 이 파일이 필요한가: 프론트는 지금까지 개발 PC에서 npm run build 뒤 next start로만 떴다.
# 이 파일이 빌드 결과(.next)와 실행에 필요한 패키지만 든 상자를 만든다. 소스와 개발용 패키지는 남지 않는다.
#
# 백엔드 주소는 구울 때 정해진다. next.config.ts의 rewrites가 next build 때 계산돼
# .next/routes-manifest.json에 굳는다. 띄울 때 BACKEND_URL을 줘도 바뀌지 않는다
# (2026-10-01 실측. 다른 주소를 주고 next start 해도 굽던 때의 주소로 넘겼다).
# 그래서 주소를 빌드 인자로 받는다. 기본값은 compose 안에서 백엔드를 부르는 이름이다.
#
# 실행 예시 (보통은 저장소 루트의 compose.yaml이 이 파일을 부른다)
#   docker build -t o2o-frontend --build-arg BACKEND_URL=http://backend:8080 frontend
#   docker compose --env-file backend/.env up --wait --build
#
# 실패 시 출력 예시
#   Failed to proxy http://localhost:8080/api/v1/... ECONNREFUSED
#     BACKEND_URL 없이 구웠다. 컨테이너 안의 localhost는 프론트 자신이다. 빌드 인자를 주고 다시 굽는다
#   npm error `npm ci` can only install packages when your package.json and package-lock.json are in sync
#     두 파일이 어긋났다. PC에서 npm install로 맞춘 뒤 다시 굽는다

FROM node:22-bookworm-slim AS deps
WORKDIR /app
# 패키지 설치를 소스 복사보다 앞에 둔다. 소스만 바뀐 빌드는 이 층을 다시 받지 않는다
COPY package.json package-lock.json ./
RUN npm ci

FROM deps AS build
ARG BACKEND_URL=http://backend:8080
ENV BACKEND_URL=$BACKEND_URL
ENV NEXT_TELEMETRY_DISABLED=1
COPY . .
RUN npm run build

FROM node:22-bookworm-slim
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

# 실행에 필요한 패키지만 다시 설치한다. 테스트 도구와 타입 검사 도구는 빠진다
COPY package.json package-lock.json ./
# 내려받기 캐시(116MB, 2026-10-01 실측)는 설치가 끝나면 쓸 일이 없어 같은 층에서 비운다
RUN npm ci --omit=dev && npm cache clean --force

COPY next.config.ts ./
COPY --from=build /app/.next ./.next

# node 이미지에 들어 있는 일반 계정. root로 돌리지 않는다
USER node

EXPOSE 3000
# 컨테이너 밖에서 들어오는 요청을 받으려면 모든 주소에서 듣는다
CMD ["node_modules/.bin/next", "start", "-H", "0.0.0.0", "-p", "3000"]
