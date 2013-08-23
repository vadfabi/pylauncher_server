#ifndef _UTILITYFN_H
#define _UTILITYFN_H

#include <string>
#include <cstdarg>
#include <string.h>
#include <arpa/inet.h>


//  A Collection of Useful Utility Functions
//  mostly copied from stack overflow
//  (some citations missing)


//  get_line
//  get a line of text from the file stream
//  return pointer to the buffer you read into
//  readIntoBuffer will be guaranteed to be null terminated
inline char *get_line (char *readIntoBuffer, size_t n, FILE *f)
{
	//  call fgets to read from file stream
	char *p = fgets (readIntoBuffer, n, f);

	//  if we read something
	if (p != 0) 
	{
		//  force it to be null terminated
		size_t last = strlen (readIntoBuffer) - 1;

		if (readIntoBuffer[last] == '\n') 
			readIntoBuffer[last] = '\0';
	}

	return p;
}



//	std::string format()
//  replaces missing string printf   (this is safe and convenient but not exactly efficient )
//  can be used like:  std::string mystr = format("%s %d %10.5f", "omg", 1, 10.5);
//  see http://stackoverflow.com/users/642882/piti-ongmongkolkul
inline std::string format(const char* fmt, ...){
	int size = 512;
	char* buffer = 0;
	buffer = new char[size];
	va_list vl;
	va_start(vl, fmt);
	int nsize = vsnprintf(buffer, size, fmt, vl);
	if(size<=nsize){ //fail delete buffer and try again
		delete[] buffer;
		buffer = 0;
		buffer = new char[nsize+1]; //+1 for /0
		nsize = vsnprintf(buffer, size, fmt, vl);
	}
	std::string ret(buffer);
	va_end(vl);
	delete[] buffer;
	return ret;
}



//  FormatTime
//  as described here
//   http://stackoverflow.com/questions/2408976/struct-timeval-to-printable-format
inline std::string FormatTime(const struct timeval &timeToFormat)
{

	time_t nowtime;
	struct tm *nowtm;
	char tmbuf[64], buf[64];
	nowtime = timeToFormat.tv_sec;
	nowtm = localtime(&nowtime);
	//strftime(tmbuf, sizeof tmbuf, "%Y-%m-%d %H:%M:%S", nowtm);
	strftime(tmbuf, sizeof tmbuf, "%H:%M:%S", nowtm);
	snprintf(buf, sizeof buf, "%s.%03d", tmbuf, (int)(timeToFormat.tv_usec/1000));

	return std::string(buf);
}


//  DurationMilliseconds
//  returns difference between tstart and tend in milliseconds
inline long DurationMilliseconds(timeval tstart, timeval tend)
{
	long startSeconds, startUseconds;
	long endSeconds, endUSeconds;

	long seconds = tend.tv_sec - tstart.tv_sec;
	long useconds = tend.tv_usec - tstart.tv_usec;

	double duration = seconds*1000.0 + useconds/1000.0;
	return (long)duration;
}


//  IpAddressString
//  returns dots and numbers format IPv4 address
inline std::string IpAddressString(const struct sockaddr_in &address)
{
	//  create string from 
	char* addressBuffer = inet_ntoa(address.sin_addr);
	return std::string(addressBuffer);
}





//  debug trace function
inline void DEBUG_TRACE(std::string traceString)
{
#ifdef DEBUG
	printf("%s\n", traceString.c_str());
#endif
}


#endif // _UTILITYFN_H