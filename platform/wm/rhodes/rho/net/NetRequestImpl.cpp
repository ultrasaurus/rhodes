#include "stdafx.h"

#include "NetRequestImpl.h"
#include "common/RhoFile.h"
#include "NetRequest.h"

#if defined(_WIN32_WCE)
#include <connmgr.h>
#endif

#ifdef OS_WINCE
extern "C" int strnicmp( const char *s1, const char *s2, size_t count );
#endif

namespace rho {
namespace net {
IMPLEMENT_LOGCLASS(CNetRequestImpl,"Net");

static boolean isLocalHost(const char* szUrl)
{
    return strnicmp(szUrl, "http://localhost", strlen("http://localhost")) == 0 ||
        strnicmp(szUrl, "http://127.0.0.0", strlen("http://127.0.0.0")) == 0;
}

CNetRequestImpl::CNetRequestImpl(CNetRequest* pParent, const char* method, const String& strUrl)
{
    m_pParent = pParent;
    m_pParent->m_pCurNetRequestImpl = this;

    pszErrFunction = NULL;
    hInet = NULL, hConnection = NULL, hRequest = NULL;
    memset(&uri, 0, sizeof(uri) );
    m_strUrl = strUrl;
    CAtlStringW strUrlW(strUrl.c_str());

    do 
    {
        if ( !isLocalHost(strUrl.c_str()) && !SetupInternetConnection(strUrlW) )
        {
            pszErrFunction = L"SetupInternetConnection";
            break;
        }

        hInet = InternetOpen(_T("rhodes-wm"), INTERNET_OPEN_TYPE_PRECONFIG, NULL, NULL, NULL );
        if ( !hInet ) 
        {
            pszErrFunction = L"InternetOpen";
            break;
        }

        DWORD dwUrlLength = 1024;
        CAtlStringW strCanonicalUrlW;
        if ( !InternetCanonicalizeUrl( strUrlW, strCanonicalUrlW.GetBuffer(dwUrlLength), &dwUrlLength, 0) )
        {
            pszErrFunction = _T("InternetCanonicalizeUrl");
            break;
        }
        strCanonicalUrlW.ReleaseBuffer();

		alloc_url_components( &uri, strCanonicalUrlW );
        if( !InternetCrackUrl( strCanonicalUrlW, strCanonicalUrlW.GetLength(), 0, &uri ) ) 
        {
			pszErrFunction = L"InternetCrackUrl";
			break;
		}

        hConnection = InternetConnect( hInet, uri.lpszHostName, uri.nPort, _T("anonymous"), NULL, 
          INTERNET_SERVICE_HTTP, 0, 0 );
        if ( !hConnection ) 
        {
            pszErrFunction = L"InternetConnect";
            break;
        }

        strReqUrlW = uri.lpszUrlPath;
        strReqUrlW += uri.lpszExtraInfo;
        hRequest = HttpOpenRequest( hConnection, CAtlStringW(method), strReqUrlW, NULL, NULL, NULL, 
          INTERNET_FLAG_KEEP_CONNECTION|INTERNET_FLAG_NO_CACHE_WRITE, NULL );
        if ( !hRequest ) 
        {
            pszErrFunction = L"HttpOpenRequest";
            break;
        }

    }while(0);
}

CNetResponseImpl* CNetRequestImpl::sendString(const String& strBody)
{
    CNetResponseImpl* pNetResp = new CNetResponseImpl;

    do
    {
        if ( isError() )
            break;

        CAtlStringW strHeaders = L"Content-Type: application/x-www-form-urlencoded\r\n";
        if ( !HttpAddRequestHeaders( hRequest, strHeaders, -1, HTTP_ADDREQ_FLAG_ADD|HTTP_ADDREQ_FLAG_REPLACE ) )
        {
            pszErrFunction = L"HttpAddRequestHeaders";
            break;
        }

        if ( !HttpSendRequest( hRequest, NULL, 0, const_cast<char*>(strBody.c_str()), strBody.length() ) )
        {
            pszErrFunction = L"HttpSendRequest";
            break;
        }

        readResponse(pNetResp);
        if ( isError() )
            break;

        readInetFile(hRequest,pNetResp);
    }while(0);

    return pNetResp;
}

void CNetRequestImpl::readResponse(CNetResponseImpl* pNetResp)
{
    DWORD dwLen = 10;
    wchar_t szHttpRes[10];
    DWORD nIndex = 0;

    if( !HttpQueryInfo( hRequest, HTTP_QUERY_STATUS_CODE, szHttpRes, &dwLen, &nIndex) )
    {
        pszErrFunction = L"HttpSendRequest";
        return;
    }
    int nCode = _wtoi(szHttpRes);
    pNetResp->setResponseCode(nCode);

    if ( nCode != 200 )
    {
        LOG(ERROR) + "An error occured connecting to the sync source: " + szHttpRes + " returned.";

        // If we're unauthorized, delete any cookies that might have been
        // stored so we don't reuse them later
        if ( nCode == 401 ) 
        {
            CAtlStringA strUrlA;
            int nQuest = strReqUrlW.Find('?'); 
            if ( nQuest > 0 )
                strUrlA = strReqUrlW.Mid(0,nQuest-1);
            else
                strUrlA = strReqUrlW;

            ::InternetSetCookieA(strUrlA, NULL, "");
        }
	}
}

CNetResponseImpl* CNetRequestImpl::downloadFile(common::CRhoFile& oFile)
{
    CNetResponseImpl* pNetResp = new CNetResponseImpl;

    do
    {
        if ( isError() )
            break;

        if ( !HttpSendRequest( hRequest, NULL, 0, NULL, 0 ) )
        {
            pszErrFunction = L"HttpSendRequest";
            break;
        }

        readResponse(pNetResp);
        if ( isError() )
            break;

        readInetFile(hRequest,pNetResp, &oFile);

    }while(0);

    return pNetResp;
}

static const char* szMultipartPrefix = 
   "------------A6174410D6AD474183FDE48F5662FCC5\r\n"
   "Content-Disposition: form-data; name=\"blob\"; filename=\"doesnotmatter.png\"\r\n"
   "Content-Type: application/octet-stream\r\n\r\n";
static const char* szMultipartPostfix = 
    "\r\n------------A6174410D6AD474183FDE48F5662FCC5--";

static const wchar_t* szMultipartContType = 
    L"Content-Type: multipart/form-data; boundary=----------A6174410D6AD474183FDE48F5662FCC5\r\n";

CNetResponseImpl* CNetRequestImpl::sendStream(common::InputStream* bodyStream)
{
    CNetResponseImpl* pNetResp = new CNetResponseImpl;

    do
    {
        if ( isError() )
            break;

        if ( !HttpAddRequestHeaders( hRequest, szMultipartContType, -1, HTTP_ADDREQ_FLAG_ADD|HTTP_ADDREQ_FLAG_REPLACE ) )
        {
            pszErrFunction = L"HttpAddRequestHeaders";
            break;
        }

	    INTERNET_BUFFERS BufferIn;
        memset(&BufferIn, 0, sizeof(INTERNET_BUFFERS));
	    BufferIn.dwStructSize = sizeof( INTERNET_BUFFERS ); // Must be set or error will occur
        BufferIn.dwBufferTotal = bodyStream->available() + strlen(szMultipartPrefix) + strlen(szMultipartPostfix);

        if(!HttpSendRequestEx( hRequest, &BufferIn, NULL, 0, 0))
        {
            pszErrFunction = L"HttpSendRequestEx";
            break;
        }

	    DWORD dwBytesWritten = 0;
        if ( !InternetWriteFile( hRequest, szMultipartPrefix, strlen(szMultipartPrefix), &dwBytesWritten) )
        {
            pszErrFunction = L"InternetWriteFile";
            break;
        }

        DWORD dwBufSize = 4096;
        char* pBuf = (char*)malloc(dwBufSize);
        int nReaded = 0;

	    do
	    {
            nReaded = bodyStream->read(pBuf,0,dwBufSize);
            if ( nReaded > 0 )
            {
		        if ( !InternetWriteFile( hRequest, pBuf, nReaded, &dwBytesWritten) )
                {
                    pszErrFunction = L"InternetWriteFile";
                    break;
                }
            }
	    }while(nReaded > 0);

        free(pBuf);

        if ( !InternetWriteFile( hRequest, szMultipartPostfix, strlen(szMultipartPostfix), &dwBytesWritten) )
        {
            pszErrFunction = L"InternetWriteFile";
            break;
        }

        if ( !HttpEndRequest(hRequest, NULL, 0, 0) )
        {
            pszErrFunction = L"HttpEndRequest";
            break;
        }

        if ( isError() )
            break;

        readResponse(pNetResp);
        if ( isError() )
            break;

        readInetFile(hRequest,pNetResp);

    }while(0);

    return pNetResp;
}

void CNetRequestImpl::close()
{
	if (pszErrFunction)
		ErrorMessage(pszErrFunction);

    free_url_components(&uri);

	if ( hRequest ) 
        InternetCloseHandle(hRequest);
	if ( hConnection ) 
        InternetCloseHandle(hConnection);
	if ( hInet ) 
        InternetCloseHandle(hInet);

    memset(&uri, 0, sizeof(uri));

    hRequest = 0;
    hConnection = 0;
    hInet = 0;
}

CNetRequestImpl::~CNetRequestImpl()
{
    close();
    m_pParent->m_pCurNetRequestImpl = null;
}

void CNetRequestImpl::readInetFile( HINTERNET hRequest, CNetResponseImpl* pNetResp, common::CRhoFile* pFile /*=NULL*/ )
{
    if ( pNetResp->getRespCode() == 500 )
        return;

    DWORD dwBufSize = 4096;
    char* pBuf = (char*)malloc(dwBufSize);
    DWORD dwBytesRead = 0;
    BOOL bRead = FALSE;
    do
    {
        bRead = InternetReadFile(hRequest, pBuf, dwBufSize, &dwBytesRead);
        if ( !bRead )
        {
            pszErrFunction = L"InternetReadFile";
            break;
        }

        if (dwBytesRead > 0)
        {
            if ( pFile )
                pFile->write(pBuf,dwBytesRead);
            else
                pNetResp->getRawData().append(pBuf,dwBytesRead);
        }

        pNetResp->setValid(true);

    }while(bRead && dwBytesRead > 0);

    free(pBuf);
}

void CNetRequestImpl::ErrorMessage(LPCTSTR pszFunction)
{ 
    // Retrieve the system error message for the last-error code
    LPTSTR pszMessage = NULL;
    DWORD dwLastError = GetLastError(); 

    DWORD dwLen = FormatMessage(
        FORMAT_MESSAGE_ALLOCATE_BUFFER | 
        //FORMAT_MESSAGE_FROM_SYSTEM |
        FORMAT_MESSAGE_FROM_HMODULE|
        FORMAT_MESSAGE_IGNORE_INSERTS,
        GetModuleHandle( _T("wininet.dll") ),
        dwLastError,
        MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
        (LPTSTR)&pszMessage,
        0, NULL );

    CAtlStringW strExtError = L"";
    if ( dwLastError == ERROR_INTERNET_EXTENDED_ERROR )
    {
        DWORD  dwInetError =0, dwExtLength = 0;
        InternetGetLastResponseInfo( &dwInetError, NULL, &dwExtLength );

        if ( dwExtLength > 0 )
        {
            InternetGetLastResponseInfo( &dwInetError, strExtError.GetBuffer(dwExtLength+1), &dwExtLength );
            strExtError.ReleaseBuffer();
        }
    }

    rho::LogMessage oLogMsg(__FILE__, __LINE__, L_ERROR, LOGCONF(), getLogCategory() );
    oLogMsg + "Call " + pszFunction + " failed. Url:" + m_strUrl.c_str() + ". With code : " + dwLastError;

    if ( pszMessage ) 
        oLogMsg + ".Message: " + pszMessage;
    if ( strExtError.GetLength() )
        oLogMsg + ".Extended info: " + strExtError.GetString();

    if ( pszMessage )
        LocalFree(pszMessage);
}

void CNetRequestImpl::alloc_url_components(URL_COMPONENTS *uri, const wchar_t *url) 
{
  int dwLength = wcslen(url)*sizeof(wchar_t);
  memset(uri, 0, sizeof(URL_COMPONENTS));

  uri->dwStructSize = sizeof(URL_COMPONENTS);
  uri->lpszScheme = (LPWSTR)malloc(dwLength);
  uri->dwSchemeLength = dwLength;
  uri->lpszHostName = (LPWSTR)malloc(dwLength);
  uri->dwHostNameLength = dwLength;
  uri->lpszUserName = (LPWSTR)malloc(dwLength);
  uri->dwUserNameLength = dwLength;
  uri->lpszPassword = (LPWSTR)malloc(dwLength);
  uri->dwPasswordLength = dwLength;
  uri->lpszUrlPath = (LPWSTR)malloc(dwLength);
  uri->dwUrlPathLength = dwLength;
  uri->lpszExtraInfo = (LPWSTR)malloc(dwLength);
  uri->dwExtraInfoLength = dwLength;
}

void CNetRequestImpl::free_url_components(URL_COMPONENTS *uri) 
{
  free(uri->lpszScheme);
  free(uri->lpszHostName);
  free(uri->lpszUserName);
  free(uri->lpszPassword);
  free(uri->lpszUrlPath);
  free(uri->lpszExtraInfo);
}

bool CNetRequestImpl::SetupInternetConnection(LPCTSTR url)
{
#if defined (_WIN32_WCE)
	int iNetwork;
	HRESULT hResult = E_FAIL;
	DWORD   dwStatus;
    static HANDLE hConnection = NULL;

	// cleanup the old connection
	if(NULL != hConnection)
	{
		hResult = ConnMgrConnectionStatus( hConnection, &dwStatus );
		if( SUCCEEDED(hResult) )
		{
			LOG(INFO) + "Internet connection exist, use it";
			if( dwStatus & CONNMGR_STATUS_CONNECTED )
				return true;
		}
		ConnMgrReleaseConnection(hConnection, FALSE);
		LOG(INFO) + "Internet connection droped, open new one";
		hConnection = NULL;
	}

	// get the right network to connect to
	iNetwork = 0;
	//CONNMGR_DESTINATION_INFO DestInfo;

	GUID pguid;
	if( FAILED( ConnMgrMapURL(url, &pguid, NULL) ) )
		return false;

	//while( SUCCEEDED(ConnMgrEnumDestinations(iNetwork++, &DestInfo)))
	{	
		LOG(INFO) + "Try establish Internet connection";
		// actually try to establish the connection
		CONNMGR_CONNECTIONINFO ConnInfo;

		ZeroMemory(&ConnInfo, sizeof(ConnInfo));
		ConnInfo.cbSize = sizeof(ConnInfo);
		ConnInfo.dwParams = CONNMGR_PARAM_GUIDDESTNET;
		ConnInfo.dwPriority = CONNMGR_PRIORITY_HIPRIBKGND;//CONNMGR_PRIORITY_USERBACKGROUND;
#if ( _WIN32_WCE >= 0x500 )
		ConnInfo.dwFlags = CONNMGR_FLAG_NO_ERROR_MSGS;
#endif
		ConnInfo.guidDestNet = pguid;

		hResult = ConnMgrEstablishConnection(&ConnInfo, &hConnection);

		// check to see if the attempt failed
		int count = 0;
		while(SUCCEEDED(hResult) && count++ < 60 )
		{
			LOG(INFO) + "Wait for connect (" + count + ")";
			DWORD dwResult = WaitForSingleObject(hConnection, 1000); 
			if (dwResult == (WAIT_OBJECT_0))
			{ 
				hResult=ConnMgrConnectionStatus(hConnection,&dwStatus);
				if( SUCCEEDED(hResult) )
				{
					if( dwStatus & CONNMGR_STATUS_CONNECTED )
					{
						LOG(INFO) + "Connected";
						return true;
					}
					if( dwStatus & CONNMGR_STATUS_WAITINGCONNECTION )
					{
						continue;
					}
					break;
				}
			}
		}
	}
	LOG(ERROR) + "Failed to connect";
	return false;
#else
	return true;
#endif //_WIN32_WCE
}

}
}
