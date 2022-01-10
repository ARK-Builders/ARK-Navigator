#[macro_use] extern crate log;
#[cfg(target_os="android")]
#[allow(non_snake_case)]
pub mod android {
    extern crate jni;

    use jni::JNIEnv;
    use jni::objects::{JString, JClass};
    use jni::sys::{jlong};
    
    use std::fs;
    use std::path::Path;
    use std::io::{BufReader, BufRead};
    use crc32fast::Hasher;

    extern crate android_logger;
    use log::Level;
    use android_logger::Config;

    #[no_mangle]
    pub unsafe extern fn Java_space_taran_arknavigator_mvp_model_repo_index_ResourceMeta_00024Companion_helloWorld(
        _env: JNIEnv,
        _: JClass) {

        android_logger::init_once(
            Config::default().with_min_level(Level::Trace));
        trace!("this is a verbose {}", "message");
    }

    #[no_mangle]
    pub unsafe extern fn Java_space_taran_arknavigator_mvp_model_repo_index_ResourceIdKt_computeIdNative(
        env: JNIEnv,
        _: JClass,
        jni_size: i64,
        jni_file_name: JString) -> jlong {
        android_logger::init_once(
            Config::default().with_min_level(Level::Trace));
        let size: usize = usize::try_from(jni_size)
            .expect(&format!("Failed to parse input size"));
        trace!("Received size: {}", size);
        let file_name: String =
            env.get_string(jni_file_name)
            .expect("Failed to parse input file name").into();
        trace!("Received filename: {}", file_name);
        let file: &Path = Path::new(&file_name);
        const KILOBYTE: usize = 1024;
        const MEGABYTE: usize = 1024 * KILOBYTE;
        const BUFFER_CAPACITY: usize = 512 * KILOBYTE;
        trace!("Calculating hash of {} (size is {} megabytes)", file.display(), size / MEGABYTE);
        let source = fs::OpenOptions::new()
            .read(true)
            .open(file)
            .expect(&format!("Failed to read from {}", file.display()));
        let mut reader = BufReader::with_capacity(BUFFER_CAPACITY, source);
        assert!(reader.buffer().is_empty());
        let mut hasher = Hasher::new();
        let mut bytes_read: usize = 0;
        loop {
            let bytes_read_iteration = reader
                .fill_buf()
                .expect(&format!("Failed to read from {}", file.display()))
                .len();
            if bytes_read_iteration == 0 {
                break;
            }
            hasher.update(reader.buffer());
            reader.consume(bytes_read_iteration);
            bytes_read += bytes_read_iteration;
        }
        let checksum: jlong  = hasher.finalize().into();
        trace!("{} bytes has been read", bytes_read);
        trace!("checksum: {:#02x}", checksum);
        assert!(bytes_read == size);
        return checksum;
    }
}
