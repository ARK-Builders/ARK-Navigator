mod utils {
    
    use std::fs;
    use std::path::Path;
    use std::io::{BufReader, BufRead};
    use crc32fast::Hasher;
    
    pub fn compute_id<PathRef: AsRef<Path>>(size: usize, file: &PathRef) -> u32 {
        const KILOBYTE: usize = 1024;
        const MEGABYTE: usize = 1024 * KILOBYTE;
        const BUFFER_CAPACITY: usize = 512 * KILOBYTE;
        println!("calculating hash of {} (size is {} megabytes)", file.as_ref().display(), size / MEGABYTE);
        let source = fs::OpenOptions::new()
            .read(true)
            .open(file)
            .expect(&format!("Failed to read from {}", file.as_ref().display()));
        let mut reader = BufReader::with_capacity(BUFFER_CAPACITY, source);
        assert!(reader.buffer().is_empty());
        let mut hasher = Hasher::new();
        let mut bytes_read: usize = 0;
        loop {
            let bytes_read_iteration = reader
                .fill_buf()
                .expect(&format!("Failed to read from {}", file.as_ref().display()))
                .len();
            if bytes_read_iteration == 0 {
                break;
            }
            hasher.update(reader.buffer());
            reader.consume(bytes_read_iteration);
            bytes_read += bytes_read_iteration;
        }
        let checksum: u32 = hasher.finalize();
        println!("{} bytes has been read", bytes_read);
        assert!(bytes_read == size);
        return checksum;
    }

#[cfg(test)]
    #[test]
    fn check_crc32() {
        const INPUT_FILE_NAME: &'static str = "assets/crc32_sample_file.bin";
        let input_file_path = Path::new(INPUT_FILE_NAME);
        let input_file_metadata = fs::metadata(INPUT_FILE_NAME)
            .expect(&format!("Failed to read metadata from from {}", input_file_path.display()));
        let input_file_size = usize::try_from(input_file_metadata.len())
                .expect(&format!("Unknown file size {}", input_file_path.display()));
        let file_checksum: u32 = compute_id(
            input_file_size,
            &input_file_path);
        assert_eq!(0xae614f37, file_checksum);
    }
}
