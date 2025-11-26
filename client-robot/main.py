import requests
import uuid
from config import SERVER_URL, ROBOT_ID
from security.crypto_manager import CryptoManager

def run_simulation():
    print(f"🤖 [Python AGV: {ROBOT_ID}] 시뮬레이션 시작...")
    
    crypto = CryptoManager()
    session = requests.Session()

    # ---------------------------------------------------
    # Step 0. 저장소 생성 (매번 새로운 이름으로 충돌 방지)
    # ---------------------------------------------------
    print("\n--- Step 0. 저장소 생성 ---")
    unique_name = f"Python-Repo-{str(uuid.uuid4())[:8]}"
    
    resp = session.post(f"{SERVER_URL}/repositories", json={
        "name": unique_name,
        "description": "파이썬 클라이언트 자동 생성",
        "ownerId": ROBOT_ID
    })
    
    if resp.status_code != 200:
        print(f"❌ 저장소 생성 실패: {resp.text}")
        return
        
    repo_id = resp.json()
    print(f"✅ 저장소 생성 완료! ID: {repo_id}")

    # ---------------------------------------------------
    # Step 1. 키 교환 (Handshake)
    # ---------------------------------------------------
    print("\n--- Step 1. 보안 핸드셰이크 ---")
    my_key_pair = crypto.generate_client_key_pair()
    public_key_json = crypto.get_public_key_json(my_key_pair)

    resp = session.post(f"{SERVER_URL}/security/handshake", json={
        "publicKeyJson": public_key_json
    })

    if resp.status_code != 200:
        print(f"❌ 핸드셰이크 실패: {resp.text}")
        return

    # 서버 응답에서 AES 키 복구
    encrypted_aes_key = resp.json()['encryptedAesKey']
    aes_handle = crypto.unwrap_aes_key(encrypted_aes_key, my_key_pair)
    print("✅ AES 키 수신 및 복구 성공!")

    # ---------------------------------------------------
    # Step 2. 데이터 암호화 및 업로드
    # ---------------------------------------------------
    print("\n--- Step 2. 데이터 암호화 업로드 ---")
    original_data = "Target Coordinates: [37.5665, 126.9780]"
    encrypted_content = crypto.encrypt_data(original_data, aes_handle)

    print(f"원본: {original_data}")
    print(f"암호문: {encrypted_content[:30]}...")

    resp = session.post(f"{SERVER_URL}/documents", json={
        "content": encrypted_content,
        "repositoryId": repo_id
    })
    print(f"🎉 업로드 결과: {resp.text}")

    # ID 추출 (숫자만)
    doc_id = ''.join(filter(str.isdigit, resp.text))

    # ---------------------------------------------------
    # Step 3. 다운로드 및 검증
    # ---------------------------------------------------
    print("\n--- Step 3. 다운로드 및 복호화 검증 ---")
    resp = session.get(f"{SERVER_URL}/documents/{doc_id}")
    
    downloaded_content = resp.json()['content']
    decrypted_data = crypto.decrypt_data(downloaded_content, aes_handle)
    
    print(f"🔓 복호화된 데이터: {decrypted_data}")

    if original_data == decrypted_data:
        print("\n✨ [SUCCESS] 완벽하게 일치합니다! ✨")
    else:
        print("\n❌ [FAILED] 데이터가 다릅니다!")

if __name__ == "__main__":
    run_simulation()
