"""
TourAPI searchKeyword2 로 각 커뮤니티 게시글에 어울리는 이미지를 검색하고
db/seed_community.sql 의 post_images 블록을 실제 URL 로 교체한다.
"""

import urllib.request
import urllib.parse
import json
import re
import sys

API_KEY  = "73c339a232ae3d694d3f76d524933ae3fda8c253c457a71fa3cf0bfe73609c7c"
BASE_URL = "https://apis.data.go.kr/B551011/KorService2/searchKeyword2"

# 게시글 ID → 검색 키워드 (내용에 가장 어울리는 관광지/장소)
KEYWORDS = {
    1:  "성산일출봉",
    2:  "해운대해수욕장",
    3:  "불국사",
    4:  "경포해수욕장",
    5:  "향일암",
    6:  "한라산",
    7:  "광안리해수욕장",
    8:  "황리단길",
    9:  "안목해변",
    10: "우도",
    11: "제주도",
    12: "해운대",
    13: "서울역",
    14: "인천국제공항",
    15: "명동",
    16: "협재해수욕장",
    17: "자갈치시장",
    18: "첨성대",
    19: "경포호수",
    20: "돌산공원",
}

def fetch_image(keyword: str) -> str | None:
    params = urllib.parse.urlencode({
        "serviceKey": API_KEY,
        "numOfRows":  10,
        "pageNo":     1,
        "MobileOS":   "ETC",
        "MobileApp":  "TripApp",
        "_type":      "json",
        "keyword":    keyword,
    })
    url = f"{BASE_URL}?{params}"
    try:
        with urllib.request.urlopen(url, timeout=10) as resp:
            data = json.loads(resp.read().decode())
        items_wrap = data["response"]["body"]["items"]
        # 결과 없으면 빈 문자열로 오는 경우 있음
        if not items_wrap or isinstance(items_wrap, str):
            return None
        items = items_wrap["item"]
        if isinstance(items, dict):
            items = [items]
        for item in items:
            img = item.get("firstimage", "").strip()
            if img:
                return img
        return None
    except Exception as e:
        print(f"  [ERROR] {keyword}: {e}", file=sys.stderr)
        return None

def main():
    results: dict[int, str] = {}
    for post_id, keyword in KEYWORDS.items():
        print(f"  post {post_id:2d} [{keyword}] ... ", end="", flush=True)
        url = fetch_image(keyword)
        if url:
            results[post_id] = url
            print(f"OK  {url[:80]}")
        else:
            results[post_id] = f"https://picsum.photos/seed/{keyword}/800/600"
            print(f"NOT FOUND → picsum fallback")

    # seed_community.sql 의 post_images INSERT 블록을 교체
    sql_path = "db/seed_community.sql"
    with open(sql_path, encoding="utf-8") as f:
        sql = f.read()

    def replace_image_row(m: re.Match) -> str:
        row_id   = int(m.group(1))
        disp_ord = m.group(3)
        post_id  = int(m.group(4))
        new_url  = results.get(post_id, m.group(2))
        return f"({row_id},  '{new_url}', {disp_ord}, {post_id})"

    # INSERT 행 패턴: (id, 'url', order, post_id)
    pattern = r"\((\d+),\s+'([^']+)',\s+(\d+),\s+(\d+)\)"
    new_sql  = re.sub(pattern, replace_image_row, sql)

    with open(sql_path, "w", encoding="utf-8") as f:
        f.write(new_sql)

    print(f"\n{sql_path} 업데이트 완료 ({len(results)}건)")

if __name__ == "__main__":
    main()
