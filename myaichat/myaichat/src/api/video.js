import http from './http'

/** 视频列表 */
export const listVideos = () => http.get('/videos')

/** 视频详情 */
export const getVideo = (id) => http.get(`/videos/${id}`)

const toFormData = ({ videoName, videoFile, imageFile }) => {
  const formData = new FormData()
  formData.append('videoName', videoName)
  if (videoFile) formData.append('file', videoFile)
  if (imageFile) formData.append('image', imageFile)
  return formData
}

/**
 * 上传视频：大文件不设超时，并通过 onProgress 回传上传百分比
 */
export const uploadVideo = (payload, onProgress) =>
  http.post('/videos/upload', toFormData(payload), {
    timeout: 0,
    onUploadProgress: (event) => {
      if (onProgress && event.total) {
        onProgress(Math.round((event.loaded / event.total) * 100))
      }
    }
  })

/** 修改视频，未选文件则只改名称 */
export const updateVideo = (id, payload, onProgress) =>
  http.put(`/videos/${id}`, toFormData(payload), {
    timeout: 0,
    onUploadProgress: (event) => {
      if (onProgress && event.total) {
        onProgress(Math.round((event.loaded / event.total) * 100))
      }
    }
  })

/** 删除视频 */
export const removeVideo = (id) => http.delete(`/videos/${id}`)
