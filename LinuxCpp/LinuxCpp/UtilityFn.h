#ifndef _UTILITYFN_H
#define _UTILITYFN_H

#include <string>
#include <cstdarg>

//	string format function
//  replaces missing string printf   (this is safe and convenient but not exactly efficient )
//  can be used like:  std::string mystr = format("%s %d %10.5f", "omg", 1, 10.5);
//  see http://stackoverflow.com/users/642882/piti-ongmongkolkul
//
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


inline long DurationMilliseconds(timeval tstart, timeval tend)
{
	long startSeconds, startUseconds;
	long endSeconds, endUSeconds;

	long seconds = tend.tv_sec - tstart.tv_sec;
	long useconds = tend.tv_usec - tstart.tv_usec;

	double duration = seconds*1000.0 + useconds/1000.0;
	return (long)duration;
}


//  debug trace function
inline void DEBUG_TRACE(std::string traceString)
{
#ifdef DEBUG
	printf("%s\n", traceString.c_str());
#endif
}


#endif // _UTILITYFN_H