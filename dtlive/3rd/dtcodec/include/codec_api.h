/*API*/

#ifndef CODEC_API_H
#define CODEC_API_H

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

enum CODEC_MEDIA_TYPE{
    CODEC_MEDIA_TYPE_UNKOWN    = -1,
    CODEC_MEDIA_TYPE_AUDIO     = 0,
    CODEC_MEDIA_TYPE_VIDEO     = 1
};

enum CODEC_MEDIA_FORMAT{
    CODEC_MEDIA_FORMAT_UNKOWN    = -1,
    // VIDEO
    CODEC_MEDIA_FORMAT_H264      = 0,
    CODEC_MEDIA_FORMAT_H265      = 1,

    //AUDIO
    CODEC_MEDIA_FORMAT_AAC       = 0X100
};

#define CODEC_ENCODER 0
#define CODEC_DECODER 1

struct codec_para
{
    int media_type;
    int media_format;
    int is_encoder;
    
    // audio para
    int samplerate;
    int channels;

    // video para
    int width;
    int height;

};

struct codec_packet
{
    int format;
    size_t size;
    int64_t pts;
    int key;
    uint8_t *data;
};

struct codec_frame
{
    int format;
    size_t size;
    int64_t pts;
    int key;
    uint8_t *data;

    // number of audio samples (per channel) described by this frame
    int nb_samples;
};


struct codec_context {
    struct codec_para para;
    void *codec; // pointer to decoder&encoder context
};

struct codec_packet;
struct codec_frame;

void codec_register_all();

struct codec_context *codec_create_codec(struct codec_para *para);
int codec_decode_frame();
int codec_encode_frame(struct codec_context *handle, struct codec_packet *pkt, struct codec_frame *frame);
int codec_get_parameter();
int codec_set_parameter();
int codec_destroy_codec(struct codec_context *handle);

#endif
