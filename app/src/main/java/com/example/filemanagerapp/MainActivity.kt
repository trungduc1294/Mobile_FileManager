package com.example.filemanagerapp
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.Locale
import android.Manifest
import android.provider.Settings
class MainActivity : AppCompatActivity() {

    private lateinit var fileList: MutableList<String>
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var currentDirectory: File
    private lateinit var textContent: TextView
    private lateinit var imageContent: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textContent = findViewById(R.id.text_content)
        imageContent = findViewById(R.id.image_content)
        fileList = mutableListOf()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, fileList)


        val listView: ListView = findViewById(R.id.list_view)
        listView.adapter = adapter
        registerForContextMenu(listView)

        Log.v("TAG", Environment.getExternalStorageDirectory().path)
        if (Build.VERSION.SDK_INT < 30) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                Log.v("TAG", "Permission Denied => Request permission")
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1234)
            } else {
                Log.v("TAG", "Permission Granted")
            }
        } else {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
        // Load files from external storage
        val externalStorageDirectory = Environment.getExternalStorageDirectory()
        currentDirectory = externalStorageDirectory
//        val externalStorageFile1 = Environment.DIRECTORY_DOCUMENTS

        displayFiles(currentDirectory)

        // Set click listener for ListView items
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedFile = File(currentDirectory, fileList[position])
            if (selectedFile.isDirectory) {
                currentDirectory = selectedFile
                displayFiles(currentDirectory)
            } else {
                if (isTextFile(selectedFile)) {
                    listView.visibility = View.GONE
                    displayTextFile(selectedFile)
                } else if (isImageFile(selectedFile)) {
                    listView.visibility = View.GONE
                    displayImage(selectedFile)
                } else {
                    showToast("File type is not support")
                }
            }
        }
    }

    private fun displayFiles(directory: File) {
        val files = directory.listFiles()

        fileList.clear()

        if (files != null) {
            Log.d("Debug", "Number of files in directory: ${files.size}")
            for (file in files) {
                fileList.add(file.name)
            }
        } else {
            Log.d("Debug", "No files found in the directory")
        }

        adapter.notifyDataSetChanged()
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun isTextFile(file: File): Boolean {
        val extension = file.extension.lowercase(Locale.getDefault())
        return extension == "txt"
    }

    private fun isImageFile(file: File): Boolean {
        val extension = file.extension.lowercase(Locale.getDefault())
        return extension == "bmp" || extension == "jpg" || extension == "png"
    }

    private fun displayTextFile(file: File) {
        try {
            val reader = BufferedReader(FileReader(file))
            val stringBuilder = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                stringBuilder.append(line).append("\n")
                line = reader.readLine()
            }
            reader.close()

            val text = stringBuilder.toString()
            imageContent.visibility = View.GONE
            textContent.visibility = View.VISIBLE
            // Show text content
            textContent.text = text

        } catch (e: IOException) {
            e.printStackTrace()
            showToast("Lỗi đọc tệp văn bản")
        }
    }




    private fun displayImage(file: File) {
        Glide.with(this).load(file).into(imageContent)

        textContent.visibility = View.GONE
        imageContent.visibility = View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.option_menu, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_folder_menu -> {
                showNewFolderDialog()
                return true
            }
            R.id.add_text_file_menu -> {
                showNewFileDialog()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)

        val inflater: MenuInflater = menuInflater
        val info = menuInfo as AdapterView.AdapterContextMenuInfo
        val selectedFile = File(currentDirectory, fileList[info.position])

        if (selectedFile.isDirectory) {
            inflater.inflate(R.menu.folder_context_menu, menu)
        } else {
            inflater.inflate(R.menu.file_context_menu, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val selectedFile = File(currentDirectory, fileList[info.position])

        when (item.itemId) {
            R.id.file_rename_menu ,R.id.rename_menu-> {
                showRenameDialog(selectedFile)
                return true
            }
            R.id.file_delete_menu, R.id.delete_menu -> {
                showDeleteDialog(selectedFile)
                return true
            }
            R.id.file_copy_menu -> {
                showCopyDialog(selectedFile)
                return true
            }
            else -> return super.onContextItemSelected(item)

        }

    }

    private fun showNewFolderDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("New Folder")
        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("Create") { _, _ ->
            val folderName = input.text.toString()
            val newFolder = File(currentDirectory, folderName)
            if (!newFolder.exists()) {
                newFolder.mkdir()
                displayFiles(currentDirectory)
            } else {
                showToast("Folder already exists")
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun showNewFileDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("New Text File")
        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("Create") { _, _ ->
            val fileName = input.text.toString() +".txt"


            val newFile = File(currentDirectory, fileName)
            if (!newFile.exists()) {
                newFile.createNewFile()
                displayFiles(currentDirectory)
            } else {
                showToast("File already exists")
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun showRenameDialog(file: File) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Rename")
        val input = EditText(this)
        input.setText(file.name)
        builder.setView(input)

        builder.setPositiveButton("Rename") { _, _ ->
            val newName = input.text.toString()
            val newFile = File(currentDirectory, newName)
            if (!newFile.exists()) {
                file.renameTo(newFile)
                displayFiles(currentDirectory)
            } else {
                showToast("File/Folder with the same name already exists")
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun showDeleteDialog(file: File) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete")
        builder.setMessage("Are you sure you want to delete ${file.name}?")

        builder.setPositiveButton("Delete") { _, _ ->
            deleteRecursive(file)
            displayFiles(currentDirectory)
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun showCopyDialog(file: File) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Copy to...")
        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("Copy") { _, _ ->
            val destinationPath = input.text.toString()
            val destinationDirectory = File(destinationPath)
            if (destinationDirectory.exists() && destinationDirectory.isDirectory) {
                val newFile = File(destinationDirectory, file.name)
                file.copyTo(newFile)
                displayFiles(currentDirectory)
            } else {
                showToast("Invalid destination path")
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            for (child in fileOrDirectory.listFiles()!!) {
                deleteRecursive(child)
            }
        }
        try {
            fileOrDirectory.delete()
            displayFiles(currentDirectory)
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Error deleting file: ${e.message}")
        }
    }
}
