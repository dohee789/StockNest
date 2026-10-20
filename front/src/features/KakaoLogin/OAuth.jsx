const CLIENT_ID  = "ef23cfa2f66fcc25924f54647f3f5460";
const REDIRECT_URI = (process.env.REACT_APP_API_URL || 'http://localhost:8090') + '/kakaologin';

export const KAKAO_AUTH_URL = `https://kauth.kakao.com/oauth/authorize?client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&response_type=code`;