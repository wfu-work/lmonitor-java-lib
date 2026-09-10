/* Reference: lmonitor-go-lib/main.go; source SHA256 560ed397e2fb848479095e02a824e339e3d438b9aae170036b70b7299dbeb794 */
#include <stdint.h>
typedef struct LMonitorStreamInfo {
	int32_t calMode;
	double es[6];
	double ee[6];
	int32_t ti;
	int32_t vrsMode;
	int32_t navsys;
	int32_t solstatic;
	double fixThresh;
	double rb[3];
	const char* outfile;
	const uint8_t* brdc_buf;
	uint64_t brdc_len;
	const uint8_t* rover_buf;
	uint64_t rover_len;
	const uint8_t* base_buf;
	uint64_t base_len;
	const uint8_t* prec_buf;
	uint64_t prec_len;
	int32_t ionoopt;
	int32_t tropopt;
	int32_t armode;
	int32_t sateph;
	int32_t nf;
	int32_t minfix;
	double warmup_min;
} LMonitorStreamInfo;
