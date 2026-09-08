---
name: 평가 지적 반영
about: A와 B 리포트의 지적을 결정표로 옮기고 반영하는 작업
title: "[반영] "
labels: review, apply
---

## 대상

| 항목 | 내용 |
|---|---|
| Task ID와 라운드 | |
| 결정표 경로 | |
| A 리포트 경로와 해시 | |
| B 리포트 경로와 해시 | |
| 원본 지적 수 A와 B | |

## G2 결과

`node harness/tools/check.mjs g2 <결정표> --mode pre` 출력을 그대로 붙인다. 요약하지 않는다.

```
```

## 반영 범위

수용한 지적 ID와 각각의 변경 위치.

## 반영하지 않은 것

거부와 반박의 ID와 이유. 치명 거부는 오판 판단 기록 위치를 함께 적는다.

## 최종 확인

`node harness/tools/check.mjs g2 <결정표> --mode final` 통과 여부. 남은 실제 치명이 있으면 미완료다.
