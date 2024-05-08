# Room of Prey Game Data Extractor
*검은방: 밀실탈출* 게임 데이터 추출기입니다. 시리즈 전체 데이터 파일 추출이 가능합니다.

## 사용법

```sh
# jar 사용 시
java -jar RoomOfPreyExtractor.jar <input_file> [<output_dir>] [<filelist_hex_hash>]

# class 사용 시
java dev.noeul.roomofprey.extractor.RoomOfPreyExtractor <input_file> [<output_dir>] [<filelist_hex_hash>]
```

참고로 `filelist_hex_hash` 인자의 기본값은 `BC909D54` 입니다.
