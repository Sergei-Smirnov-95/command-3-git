package com.company

import java.io.FileInputStream
import org.apache.commons.codec.digest.DigestUtils
import java.io.*
import java.nio.file.Files
import java.util.*


object Main {


    @Throws(IOException::class)
    fun hashDirectory(directoryPath: String, includeHiddenFiles: Boolean): String {
        val directory = File(directoryPath)
        if (!directory.isDirectory) {
            throw IllegalArgumentException("Not a directory")
        }

        val fileStreams = Vector<FileInputStream>()
        collectFiles(directory, fileStreams, includeHiddenFiles)

        SequenceInputStream(fileStreams.elements()).use { sequenceInputStream -> return DigestUtils.md5Hex(sequenceInputStream) }
    }

    @Throws(IOException::class)
    private fun collectFiles(directory: File,
                             fileInputStreams: MutableList<FileInputStream>,
                             includeHiddenFiles: Boolean) {
        val files = directory.listFiles()
        if (files != null) {
            files.sortedBy{it.name }
            //Arrays.sort(files, Comparator.comparing<File, String>(Function <File, String> { it.getName() }))
            for (file in files) {
                if (includeHiddenFiles || !Files.isHidden(file.toPath())) {
                    if (file.isDirectory) {
                        collectFiles(file, fileInputStreams, includeHiddenFiles)
                    } else {
                        fileInputStreams.add(FileInputStream(file))
                    }
                }
            }
        }
    }

    fun getFilesCount(file: File): Int {
        val files = file.listFiles()
        var count = 0
        for (f in files!!)
            if (f.isDirectory)
                count += getFilesCount(f)
            else
                count++

        return count
    }


    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val dirpath1 = "D:\\Tests\\test1"
        val dirpath2 = "D:\\Tests\\test2"

        val dir1: String
        var dir2: String
        val dirNum1: Int
        val dirNum2: Int

        dir1 = hashDirectory(dirpath1, false)
        dir2 = hashDirectory(dirpath2, false)

        dirNum1 = getFilesCount(File(dirpath1))
        dirNum2 = getFilesCount(File(dirpath2))

        val res = dir2 == dir1

        System.out.printf("Info for %s dir:\n" +
                "Hash: %s\n" +
                "Count of files: %d\n\n" +
                "Info for %s dir:\n" +
                "Hash: %s\n" +
                "Count of files: %d\n\n" +
                "Equal -> %b",
                dirpath1, dir1, dirNum1,
                dirpath2, dir2, dirNum2,
                res)

    }


}
