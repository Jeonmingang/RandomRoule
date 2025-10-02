

## BentoBox/BSkyBlock 연동 (명령·퍼미션 방식, 무의존 빌드)
- **섬 반경(보호범위) 즉시 반영**: 콘솔로 `/bsbadmin range set <소유자> <반경>` 실행
- **팀 최대 인원 적용**: `[bskyblock].team.maxsize.X` 퍼미션을 섬 소유자에게 런타임으로 부여 (접속 시 자동 재적용)
- BentoBox가 없으면 자동으로 **독립 모드**로 동작합니다.

> 참고 문서: BentoBox FAQ (섬 크기 권한/커맨드), BSkyBlock 권한 목록 (range set, display) 등.

설정(`config.yml`):
```yml
integration:
  bentobox:
    enabled: true
    gamemode-id: "bskyblock"   # 서버에 맞게 변경 가능
```


## 관리자 설정 (전부 /섬 하위, 한글)
- `/섬 설정 리로드` — 설정/블럭경험치 리로드
- `/섬 설정 보기` — 주요 설정값 확인
- `/섬 설정 블럭경험치 설정 <블럭> <xp>` — blocks.yml 수정 + 즉시 반영
- `/섬 설정 바리어 시간 <초>` — 방벽 표시 시간
- `/섬 설정 레벨 증가율 <퍼센트>` — 필요 XP 증가율(%)
- `/섬 설정 비용 size base|multiplier <값>` — 섬 크기 비용/증가율
- `/섬 설정 비용 team base|multiplier <값>` — 팀원 비용/증가율
- `/섬 설정 랭킹접두어 <형식>` — [ 섬 랭킹 <rank>위 ] 포맷
- `/섬 설정 저장` — config.yml 저장

권한: `samsky.admin` (OP 기본)
